package net.voldrich.test.graal;

import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class ScriptHandlerTest extends BaseWiremockTest {

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
                        .withFixedDelay(100)
                        .withBody(fromResource("responses/ceo-list.json"))));

        doScriptRequest("scripts/test-http-get.js")
                .expectStatus()
                .is2xxSuccessful();
    }

    @Test
    void testScriptWithHttpRequestWithHeaders() {
        String mediaType = "text/json;charset=UTF-8";
        String returnedCustomHeaderValue = "value";
        wireMock.stubFor(get(urlEqualTo("/company/info"))
                .withHeader("a", new EqualToPattern("valuea"))
                .withHeader("b", new EqualToPattern("valueb"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", mediaType)
                        .withHeader("customheader", returnedCustomHeaderValue)
                        .withBody(fromResource("responses/company-info.json"))));

        doScriptRequest("scripts/test-http-get-headers.js")
                .expectStatus()
                .is2xxSuccessful()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("200")
                .jsonPath("$.mediaType").isEqualTo(mediaType)
                .jsonPath("$.headers.customheader").isEqualTo(returnedCustomHeaderValue);
    }

    @Test
    void testScriptWithPostHttpRequest() {
        wireMock.stubFor(post(urlEqualTo("/company/add"))
                .withRequestBody(equalToJson("{ \"data\": \"valuedata\" }"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(fromResource("responses/company-info.json"))));

        doScriptRequest("scripts/test-http-post-headers.js")
                .expectStatus()
                .is2xxSuccessful()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("200");
    }

    @Test
    void testScriptWithPostHttpRequestWithHeaders() {
        String mediaType = "text/json;charset=UTF-8";
        String returnedCustomHeaderValue = "value";
        wireMock.stubFor(post(urlEqualTo("/company/add"))
                .withHeader("a", new EqualToPattern("valuea"))
                .withHeader("b", new EqualToPattern("valueb"))
                .withRequestBody(equalToJson("{ \"data\": \"valuedata\" }"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", mediaType)
                        .withHeader("customheader", returnedCustomHeaderValue)
                        .withBody(fromResource("responses/company-info.json"))));

        doScriptRequest("scripts/test-http-post-headers.js")
                .expectStatus()
                .is2xxSuccessful()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("200")
                .jsonPath("$.mediaType").isEqualTo(mediaType)
                .jsonPath("$.headers.customheader").isEqualTo(returnedCustomHeaderValue);
    }

    @Test
    void testScriptWithHttpRequest404() {
        doScriptRequest("scripts/test-http-get-404.js")
                .expectStatus()
                .is4xxClientError();
    }

    @Test
    void testScriptErrorSyntax() {
        doScriptRequest("scripts/test-script-error-syntax.js")
                .expectStatus()
                .is4xxClientError()
                .expectBody()
                .jsonPath("$.message").value(Matchers.startsWith("org.graalvm.polyglot.PolyglotException: SyntaxError: script:2:30 Expected"));
    }

    @Test
    void testScriptErrorEval() {
        doScriptRequest("scripts/test-script-error-eval.js")
                .expectStatus()
                .is4xxClientError()
                .expectBody()
                .jsonPath("$.message").value(Matchers.startsWith("ReferenceError: company is not defined"));
    }

}