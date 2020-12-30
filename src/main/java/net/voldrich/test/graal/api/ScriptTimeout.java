package net.voldrich.test.graal.api;

import lombok.extern.slf4j.Slf4j;
import net.voldrich.test.graal.script.ContextWrapper;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
public class ScriptTimeout {

    private ContextWrapper contextWrapper;

    public ScriptTimeout(ContextWrapper contextWrapper) {
        this.contextWrapper = contextWrapper;
    }

    @HostAccess.Export
    public Value ms(int timeoutMs, Value response) {
        Mono<Value> operation = Mono.delay(Duration.ofMillis(timeoutMs)).then(Mono.just(response));
        return contextWrapper.wrapMonoInPromise(operation, "Timeout for " + timeoutMs);
    }

}
