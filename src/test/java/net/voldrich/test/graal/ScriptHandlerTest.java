package net.voldrich.test.graal;

import com.github.tomakehurst.wiremock.WireMockServer;
import net.voldrich.test.graal.script.AsyncScriptExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import ru.lanwen.wiremock.ext.WiremockResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@ExtendWith(WiremockResolver.class)
@WebFluxTest(controllers = ScriptServerRouter.class)
@Import({ScriptHandler.class, AsyncScriptExecutor.class})
class ScriptHandlerTest {

    protected WireMockServer wireMock;

    @Autowired
    private WebTestClient webClient;

    @BeforeEach
    void setUp(@WiremockResolver.Wiremock WireMockServer wireMock) {
        this.wireMock = wireMock;
    }

    private String fromResource(String scriptPath) {
        try {
            File resource = new ClassPathResource(scriptPath).getFile();
            return new String(Files.readAllBytes(resource.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testScriptWithHttpRequest() {
        wireMock.stubFor(get(urlEqualTo("/company/info"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(fromResource("responses/company-info.json"))));

        wireMock.stubFor(get(urlEqualTo("/company/ceo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(fromResource("responses/ceo-list.json"))));

        webClient.post()
                .uri("/script/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .header("baseurl", wireMock.baseUrl())
                .body(BodyInserters.fromValue(fromResource("scripts/test-http-get.js")))
                .exchange()
                .expectStatus()
                .is2xxSuccessful();
    }
}