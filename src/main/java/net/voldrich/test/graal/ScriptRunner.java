package net.voldrich.test.graal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptRunner {

    private static final Logger logger = LoggerFactory.getLogger(ScriptRunner.class);

    private static final String JS_LANGUAGE_TYPE = "js";

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            runScript(Files.readString(Path.of(args[0])), args[1]);
        } else {
            System.out.println("Usage: java -jar script-runner.jar PATH_TO_SCRIPT BASE_URL");
        }
    }

    public static void runScript(String script, String baseUrl) throws IOException {
        logger.info("Script compile start");
        Source source = Source.newBuilder(JS_LANGUAGE_TYPE, script, "download-script")
                .cached(true)
                .build();
        logger.info("Script compile finished");

        try (Context context = Context.newBuilder(JS_LANGUAGE_TYPE).build()) {
            Value bindings = context.getBindings(JS_LANGUAGE_TYPE);
            bindings.putMember("client", new ScriptHttpClient(context, baseUrl));
            bindings.putMember("store", new ScriptDataStore(context));

            logger.info("Script eval start");
            Value scriptFunctions = context.eval(source);
            logger.info("Script eval finished");
            ScriptUtils.logValue(scriptFunctions, 0);

            Value functionToInvoke = scriptFunctions.getMember("rates");
            if (functionToInvoke.canExecute()) {
                functionToInvoke.execute();
            }
        }
    }


}
