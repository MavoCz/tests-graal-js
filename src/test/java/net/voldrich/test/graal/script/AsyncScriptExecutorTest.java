package net.voldrich.test.graal.script;

import net.voldrich.test.graal.api.ScriptTimeout;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static net.voldrich.test.graal.ScriptTestUtils.fromResource;
import static net.voldrich.test.graal.script.AsyncScriptExecutor.JS_LANGUAGE_TYPE;

class AsyncScriptExecutorTest {

    @Test
    void executeScript() {
        Mono<String> scriptMono = createTimeoutMono();

        StepVerifier.create(scriptMono)
                .expectNext("[{\"id\":1,\"name\":\"Steve Jobs\"},{\"id\":2,\"name\":\"Bob Balmer\"}]")
                .expectComplete()
                .verify();
    }

    @Test
    void executeScriptCancel() throws InterruptedException {
        Mono<String> scriptMono = createTimeoutMono();

        StepVerifier.create(scriptMono)
                .thenRequest(1)
                .thenAwait(Duration.ofMillis(50))
                .thenCancel()
                .verify();
    }

    private Mono<String> createTimeoutMono() {
        AsyncScriptExecutor executor = new AsyncScriptExecutor.Builder().build();
        return executor.executeScript(
                fromResource("scripts/test-script-timeout.js"),
                scriptContext -> {
                    Value bindings = scriptContext.getContext().getBindings(JS_LANGUAGE_TYPE);
                    bindings.putMember("timeout", new ScriptTimeout(scriptContext));
                });
    }


}