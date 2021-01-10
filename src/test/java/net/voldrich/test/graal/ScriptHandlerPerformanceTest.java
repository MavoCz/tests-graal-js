package net.voldrich.test.graal;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Timer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static net.voldrich.test.graal.ScriptTestUtils.fromResource;

@Disabled
public class ScriptHandlerPerformanceTest extends BaseWiremockTest {

    @Test
    void testScriptWithHttpRequest() {
        wireMock.stubFor(get(urlEqualTo("/company/info"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(50)
                        .withBody(fromResource("responses/company-info.json"))));

        wireMock.stubFor(get(urlEqualTo("/company/ceo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(50)
                        .withBody(fromResource("responses/ceo-list.json"))));

        // warmup
        runScript("scripts/test-http-get.js", 10, 100, 1000);
    }

    @Test
    void testScriptWithTimeout() {
        runScript("scripts/test-script-timeout.js", 20, 100, 2000);
    }

    @Test
    void testScriptWithSleepBlock() {
        // to compare timout implemented by blocking the script thread
        runScript("scripts/test-script-timeout-block.js", 20, 100, 20000);
    }

    private void runScript(String scriptPath, int threadCount, int warmupRequestCount, int requestCount) {
        Scheduler requestScheduler = Schedulers.fromExecutor(Executors.newFixedThreadPool(threadCount));

        runScript(() -> doScriptRequestAndCheckOk(scriptPath), warmupRequestCount, requestScheduler);
        runScript(() -> doScriptRequestWithTimer(scriptPath), requestCount, requestScheduler);

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.MILLISECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.report();
    }

    private void runScript(Supplier<WebTestClient.ResponseSpec> scriptSupplier, int requestCount, Scheduler requestScheduler) {
        Flux.range(1, requestCount)
                .flatMap(integer -> Mono.fromSupplier(scriptSupplier).subscribeOn(requestScheduler))
                .blockLast();
    }

    private WebTestClient.ResponseSpec doScriptRequestWithTimer(String scriptPath) {
        try (final Timer.Context context = requestTimer.time()) {
            return doScriptRequestAndCheckOk(scriptPath);
        }
    }
}
