package net.voldrich.test.graal;

import lombok.extern.slf4j.Slf4j;
import net.voldrich.test.graal.api.ScriptConfig;
import net.voldrich.test.graal.api.ScriptExecutionException;
import net.voldrich.test.graal.api.ScriptHttpClient;
import net.voldrich.test.graal.script.AsyncScriptExecutor;
import org.graalvm.polyglot.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.ZoneId;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static net.voldrich.test.graal.script.AsyncScriptExecutor.JS_LANGUAGE_TYPE;

@Component
@Slf4j
public class ScriptHandler {

    private static final WebClient client = WebClient.builder().build();

    private static final Engine engine = Engine.create();

    public final AsyncScriptExecutor asyncScriptExecutor;

    public ScriptHandler(AsyncScriptExecutor asyncScriptExecutor) {
        this.asyncScriptExecutor = asyncScriptExecutor;
    }

    private Context createContext(ServerRequest request) {
        Context.Builder contextBuilder = Context.newBuilder(JS_LANGUAGE_TYPE)
                .engine(engine)
                .timeZone(ZoneId.of("UTC"));

        Context context = contextBuilder.build();
        Value bindings = context.getBindings(JS_LANGUAGE_TYPE);
        bindings.putMember("client", new ScriptHttpClient(asyncScriptExecutor, context, client));
        bindings.putMember("config", new ScriptConfig(request.headers()));
        return context;
    }

    private Source createSource(String script) {
        try {
            return Source.newBuilder(AsyncScriptExecutor.JS_LANGUAGE_TYPE, script, "script")
                    .cached(true)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read and parse script", e);
        }
    }

    public Mono<ServerResponse> executeScript(ServerRequest request) {
        return request
                .bodyToMono(String.class)
                .flatMap(script -> asyncScriptExecutor.runScript(createSource(script), () -> createContext(request)))
                .flatMap(response -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(response)))
                .onErrorResume(PolyglotException.class, this::handleScriptException)
                .onErrorResume(ScriptExecutionException.class, this::handleScriptExecutionException)
                .onErrorResume(WebClientResponseException.class, exception -> Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Script execution failed " + exception.toString(), exception)));
    }

    private Mono<? extends ServerResponse> handleScriptExecutionException(ScriptExecutionException exception) {
        return Mono.error(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                exception));
    }

    private Mono<? extends ServerResponse> handleScriptException(PolyglotException exception) {
        String scriptStack = StreamSupport.stream(exception.getPolyglotStackTrace().spliterator(), false)
                .filter(PolyglotException.StackFrame::isGuestFrame)
                .map(PolyglotException.StackFrame::toString)
                .collect(Collectors.joining("\n"));
        log.debug("Script execution failed {} near {}", exception.toString(), scriptStack);
        return Mono.error(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Script execution failed: " + exception.getMessage(),
                exception));
    }

}
