package net.voldrich.test.graal.api;

import org.graalvm.polyglot.HostAccess;
import org.springframework.web.reactive.function.server.ServerRequest;

public class ScriptConfig {

    private ServerRequest.Headers headers;

    public ScriptConfig(ServerRequest.Headers headers) {
        this.headers = headers;
    }

    @HostAccess.Export
    public String get(String name) {
        return headers.firstHeader(name);
    }
}
