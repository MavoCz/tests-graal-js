package net.voldrich.test.graal;

import java.net.http.HttpResponse;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

public class ScriptHttpResponse {

    private final Context context;

    @HostAccess.Export
    public final int status;
    @HostAccess.Export
    public final String data;

    public ScriptHttpResponse(Context context, HttpResponse<String> response) {
        this.context = context;
        this.status = response.statusCode();
        this.data = response.body();
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
