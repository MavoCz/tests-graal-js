package net.voldrich.test.graal.api;

import lombok.extern.slf4j.Slf4j;
import net.voldrich.graal.async.script.ScriptContext;
import net.voldrich.graal.async.script.ScriptUtils;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
public class ScriptHttpClient {

    public static final TypeLiteral<Map<String, String>> STRING_MAP = new TypeLiteral<>() {
    };

    private final WebClient client;

    private final ScriptContext scriptContext;

    public ScriptHttpClient(ScriptContext contextWrapper, WebClient client) {
        this.scriptContext = contextWrapper;
        this.client = client;
    }

    @HostAccess.Export
    public Value get(String url) {
        return performHttpOperation(HttpMethod.GET, url, null, null);
    }

    @HostAccess.Export
    public Value get(String url, Value config) {
        return performHttpOperation(HttpMethod.GET, url, null, config);
    }

    @HostAccess.Export
    public Value post(String url, Value data) {
        return performHttpOperation(HttpMethod.POST, url, data, null);
    }

    @HostAccess.Export
    public Value post(String url, Value data, Value config) {
        return performHttpOperation(HttpMethod.POST, url, data, config);
    }

    private Value performHttpOperation(HttpMethod httpMethod, String url, Value data, Value config) {
        WebClient.RequestBodySpec requestBodySpec = client.method(httpMethod).uri(url);
        if (config != null) {
            Value headers = config.getMember("headers");
            if (headers != null) {
                headers.as(STRING_MAP).forEach(requestBodySpec::header);
            }
        }

        if (data != null) {
            if (data.isString()) {
                requestBodySpec.bodyValue(data.toString());
            } else {
                requestBodySpec.bodyValue(ScriptUtils.stringify(scriptContext.getContext(), data).toString());
            }
        }

        Mono<ScriptHttpResponse> operation = requestBodySpec.exchangeToMono(this::handleResponse)
                .doOnSubscribe(subscription -> log.info("Starting request {}", url));
        return scriptContext.executeAsPromise(operation, "HTTP " + httpMethod.name());

    }

    private Mono<ScriptHttpResponse> handleResponse(ClientResponse response) {
        log.info("Request finished with status {}", response.statusCode());
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(String.class)
                    .map(body -> new ScriptHttpResponse(scriptContext.getContext(), response, body));
        } else {
            return response.createException().flatMap(Mono::error);
        }
    }
}
