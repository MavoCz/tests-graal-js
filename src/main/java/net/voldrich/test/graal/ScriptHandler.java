package net.voldrich.test.graal;

import lombok.extern.slf4j.Slf4j;
import net.voldrich.test.graal.api.ScriptConfig;
import net.voldrich.test.graal.script.ScriptExecutionException;
import net.voldrich.test.graal.api.ScriptHttpClient;
import net.voldrich.test.graal.dto.ScriptErrorResponseDto;
import net.voldrich.test.graal.script.AsyncScriptExecutor;
import net.voldrich.test.graal.script.ContextWrapper;
import org.graalvm.polyglot.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import static net.voldrich.test.graal.script.AsyncScriptExecutor.JS_LANGUAGE_TYPE;

@Component
@Slf4j
public class ScriptHandler {

    private static final WebClient client = WebClient.builder().build();

    public final AsyncScriptExecutor asyncScriptExecutor;

    public ScriptHandler() {
        this.asyncScriptExecutor = new AsyncScriptExecutor.Builder()
                .build();
    }

    private void addContextBinding(ServerRequest request, ContextWrapper contextWrapper) {
        Value bindings = contextWrapper.getContext().getBindings(JS_LANGUAGE_TYPE);
        bindings.putMember("client", new ScriptHttpClient(contextWrapper, client));
        bindings.putMember("config", new ScriptConfig(request.headers()));
    }

    public Mono<ServerResponse> executeScript(ServerRequest request) {
        return request
                .bodyToMono(String.class)
                .flatMap(script -> asyncScriptExecutor.executeScript(
                        script,
                        contextWrapper -> addContextBinding(request, contextWrapper)))
                .flatMap(response -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(response)))
                .onErrorResume(ScriptExecutionException.class, this::handleScriptExecutionException);
    }

    private Mono<? extends ServerResponse> handleScriptExecutionException(ScriptExecutionException exception) {
        return ServerResponse
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(new ScriptErrorResponseDto(exception)));

    }

}
