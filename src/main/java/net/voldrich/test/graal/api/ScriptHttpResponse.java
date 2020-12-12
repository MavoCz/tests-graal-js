package net.voldrich.test.graal.api;

import java.net.http.HttpResponse;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.springframework.web.reactive.function.client.ClientResponse;

public class ScriptHttpResponse {

    private final Context context;

    @HostAccess.Export
    public final int status;
    @HostAccess.Export
    public final String data;

    public ScriptHttpResponse(Context context, ClientResponse response, String body) {
        this.context = context;
        this.status = response.statusCode().value();
        this.data = body;
    }

    @HostAccess.Export
    public Value json() {
        Value global = context.getBindings("js");
        return global.getMember("JSON").getMember("parse").execute(this.data);
    }

    public String text() {
        return data;
    }
}
