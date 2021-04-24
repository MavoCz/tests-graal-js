package net.voldrich.test.graal;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import ru.lanwen.wiremock.ext.WiremockResolver;

import static net.voldrich.test.graal.ScriptTestUtils.fromResource;

@ExtendWith(WiremockResolver.class)
@WebFluxTest(controllers = ScriptServerRouter.class)
@Import({ScriptRequestHandler.class})
@Slf4j
public abstract class BaseWiremockTest {
    protected WireMockServer wireMock;

    protected final MetricRegistry metrics = new MetricRegistry();
    protected Timer requestTimer = metrics.timer("requestTimer");

    @Autowired
    protected WebTestClient webClient;

    @BeforeEach
    void setUp(@WiremockResolver.Wiremock WireMockServer wireMock) {
        this.wireMock = wireMock;
    }

    protected WebTestClient.ResponseSpec doScriptRequest(String scriptPath) {
        return webClient.post()
                .uri("/script/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .header("baseurl", wireMock.baseUrl())
                .body(BodyInserters.fromValue(fromResource(scriptPath)))
                .exchange();
    }

    protected WebTestClient.ResponseSpec doScriptRequestAndCheckOk(String scriptPath) {
        return doScriptRequest(scriptPath)
                .expectStatus()
                .is2xxSuccessful();
    }
}
