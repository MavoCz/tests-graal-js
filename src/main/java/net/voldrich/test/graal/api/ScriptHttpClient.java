package net.voldrich.test.graal.api;

import lombok.extern.slf4j.Slf4j;
import net.voldrich.test.graal.script.AsyncScriptExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
public class ScriptHttpClient {

    private final WebClient client;

    private final AsyncScriptExecutor asyncScriptExecutor;

    private final Context context;

    public ScriptHttpClient(AsyncScriptExecutor asyncScriptExecutor, Context context, WebClient client) {
        this.asyncScriptExecutor = asyncScriptExecutor;
        this.context = context;
        this.client = client;
    }

    @HostAccess.Export
    public Value get(String url) {
        Mono<ScriptHttpResponse> operation = client.get().uri(url)
                .exchangeToMono(this::handleResponse)
                .doOnSubscribe(subscription -> log.info("Starting request {}", url));
        return asyncScriptExecutor.wrapMonoInPromise(context, operation);
    }

    private Mono<ScriptHttpResponse> handleResponse(ClientResponse response) {
        log.info("Request finished with status {}", response.statusCode());
        if (response.statusCode().equals(HttpStatus.OK)) {
            return response.bodyToMono(String.class)
                    .map(body -> new ScriptHttpResponse(context, response, body));
        } else if (response.statusCode().is4xxClientError()) {
            return response.createException().flatMap(Mono::error);
        } else {
            return response.createException().flatMap(Mono::error);
        }
    }
}
