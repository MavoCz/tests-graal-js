package net.voldrich.test.graal.api;

import net.voldrich.graal.async.script.ScriptUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class ScriptHttpResponse {

    private final Context context;

    @HostAccess.Export
    public final int status;
    @HostAccess.Export
    public final String data;
    @HostAccess.Export
    public final String mediaType;
    @HostAccess.Export
    public final ProxyObject cookies;
    @HostAccess.Export
    public final ProxyObject headers;

    public ScriptHttpResponse(Context context, ClientResponse response, String body) {
        this.context = context;
        this.status = response.statusCode().value();
        this.data = body;

        this.mediaType = response.headers().contentType().map(Objects::toString).orElse("");
        this.cookies = ProxyObject.fromMap(
                response.cookies().entrySet().stream().collect(
                        toMap(Map.Entry::getKey, entry -> entry.getValue().toString())));
        this.headers = ProxyObject.fromMap(
                response.headers().asHttpHeaders().keySet().stream().collect(
                        toMap(Function.identity(), key -> response.headers().header(key).stream()
                                .map(Objects::toString)
                                .collect(joining(";")))));
    }

    @HostAccess.Export
    public Value json() {
        return ScriptUtils.parseJson(context, data);
    }

    public String text() {
        return data;
    }
}
