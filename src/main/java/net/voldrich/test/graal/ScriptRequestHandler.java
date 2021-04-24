package net.voldrich.test.graal;

import lombok.extern.slf4j.Slf4j;
import net.voldrich.graal.async.script.*;
import net.voldrich.test.graal.api.ScriptConfig;
import net.voldrich.test.graal.api.ScriptTimeout;
import net.voldrich.test.graal.api.ScriptHttpClient;
import net.voldrich.test.graal.dto.ScriptErrorResponseDto;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static net.voldrich.graal.async.script.AsyncScriptExecutor.JS_LANGUAGE_TYPE;

@Component
@Slf4j
public class ScriptRequestHandler {

    private static final WebClient client = WebClient.builder().build();

    public final AsyncScriptExecutor asyncScriptExecutor;

    public ScriptRequestHandler() {
        this.asyncScriptExecutor = new AsyncScriptExecutor.Builder().build();
    }

    public Mono<ServerResponse> executeScript(ServerRequest request) {
        return request
                .bodyToMono(String.class)
                .flatMap(script -> asyncScriptExecutor.executeScript(new ScriptHandlerImpl(request, script)))
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

    private static class ScriptHandlerImpl extends BaseScriptHandler {

        private ServerRequest request;

        public ScriptHandlerImpl(ServerRequest request, String script) {
            super(ScriptUtils.parseScript(script));
            this.request = request;
        }

        @Override
        public void initiateContext(ScriptContext scriptContext) {
            Value bindings = scriptContext.getContext().getBindings(JS_LANGUAGE_TYPE);
            bindings.putMember("client", new ScriptHttpClient(scriptContext, client));
            bindings.putMember("config", new ScriptConfig(request.headers()));
            bindings.putMember("timeout", new ScriptTimeout(scriptContext));
        }
    }

}
