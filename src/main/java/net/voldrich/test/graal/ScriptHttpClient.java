package net.voldrich.test.graal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(ScriptHttpClient.class);

    private final HttpClient client = HttpClient.newHttpClient();

    private final Context context;

    private final URL baseUrl;

    public ScriptHttpClient(Context context, String baseUrl) throws MalformedURLException {
        this.context = context;
        this.baseUrl = new URL(baseUrl);
    }

    @HostAccess.Export
    public ScriptHttpResponse get(String path) throws IOException, URISyntaxException, InterruptedException {
        URL combinedUrl = new URL(baseUrl, path);
        logger.info("Starting request {}", combinedUrl);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(combinedUrl.toURI())
                .GET()
                .build();
        long start = System.currentTimeMillis();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        logger.info("Finished request {} in {} ms, status: {}",
                combinedUrl,
                System.currentTimeMillis() - start,
                response.statusCode());
        return new ScriptHttpResponse(context, response);
    }
}
