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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

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

        Scheduler requestScheduler = Schedulers.fromExecutor(Executors.newFixedThreadPool(10));

        // warmup
        Flux.range(1, 100)
                .flatMap(integer -> Mono.fromSupplier(this::doScriptRequest).subscribeOn(requestScheduler))
                .blockLast();

        Flux.range(1, 10000)
                .flatMap(integer -> Mono.fromSupplier(this::doScriptRequestWithTimer).subscribeOn(requestScheduler))
                .blockLast();

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.MILLISECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.report();

    }

    private WebTestClient.ResponseSpec doScriptRequestWithTimer() {
        try (final Timer.Context context = requestTimer.time()) {
            return doScriptRequest();
        }
    }

    protected WebTestClient.ResponseSpec doScriptRequest() {
        return doScriptRequest("scripts/test-http-get.js")
                .expectStatus()
                .is2xxSuccessful();
    }
}
