package net.voldrich.test.graal;

import net.voldrich.test.graal.api.ScriptHttpClient;
import org.graalvm.polyglot.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.ZoneId;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class ScriptHandler {

    private static final Logger logger = LoggerFactory.getLogger(ScriptHandler.class);

    private static final String JS_LANGUAGE_TYPE = "js";

    private static final WebClient client = WebClient.builder().build();

    private static final Scheduler SCRIPT_SCHEDULER = Schedulers.boundedElastic();

    private static final Engine engine = Engine.create();

    public Mono<ServerResponse> executeScript(ServerRequest request) {
        return request
                .bodyToMono(String.class)
                .flatMap(this::runScript)
                .flatMap(response -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(response)))
                .onErrorResume(PolyglotException.class, this::handleScriptException);
    }

    public Mono<String> runScript(String script) {
        return Mono.<String>create(resultSink -> {
            try {
                logger.info("Script compile start");
                Source source = Source.newBuilder(JS_LANGUAGE_TYPE, script, "download-script")
                        .cached(true)
                        .build();
                logger.info("Script compile finished");

                Context.Builder contextBuilder = Context.newBuilder(JS_LANGUAGE_TYPE)
                        .engine(engine)
                        .timeZone(ZoneId.of("UTC"));

                try (Context context = contextBuilder.build()) {
                    Value bindings = context.getBindings(JS_LANGUAGE_TYPE);
                    bindings.putMember("client", new ScriptHttpClient(context, client));

                    logger.info("Script eval start");
                    Value response = context.eval(source);
                    logger.info("Script eval finished");
                    while (response.canExecute()) {
                        response = response.execute();
                    }

                    if (!response.isString()) {
                        response = ScriptUtils.stringify(context, response);
                    }

                    resultSink.onCancel(() -> context.close(true));
                    resultSink.success(response.toString());
                }
            } catch (Exception ex) {
                resultSink.error(ex);
            }
        }).subscribeOn(SCRIPT_SCHEDULER);
    }

    private Mono<? extends ServerResponse> handleScriptException(PolyglotException exception) {
        String scriptStack = StreamSupport.stream(exception.getPolyglotStackTrace().spliterator(), false)
                .filter(PolyglotException.StackFrame::isGuestFrame)
                .map(PolyglotException.StackFrame::toString)
                .collect(Collectors.joining("\n"));
        logger.warn("Script execution failed {} near {}", exception.toString(), scriptStack);
        return Mono.error(new ResponseStatusException(
                HttpStatus.I_AM_A_TEAPOT,
                "Script execution failed " + exception.toString(), exception));
    }

}
