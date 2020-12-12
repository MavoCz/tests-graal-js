package net.voldrich.test.graal.api;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class ScriptHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(ScriptHttpClient.class);

    private final WebClient client;

    private final Context context;

    public ScriptHttpClient(Context context, WebClient client) {
        this.context = context;
        this.client = client;
    }

    @HostAccess.Export
    public ScriptHttpResponse get(String url) {
        return client.get().uri(url).exchangeToMono(this::handleResponse)
                .doOnSubscribe(subscription -> logger.info("Starting request {}", url))
                .block();
    }

    private Mono<ScriptHttpResponse> handleResponse(ClientResponse response) {
        logger.info("Request finished with status {}", response.statusCode());
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
