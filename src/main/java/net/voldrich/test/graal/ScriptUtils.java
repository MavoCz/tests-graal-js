package net.voldrich.test.graal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptUtils {
    private static final Logger logger = LoggerFactory.getLogger(ScriptRunner.class);

    public static Value stringify(Context context, Value data) {
        Value global = context.getBindings("js");
        return global.getMember("JSON").getMember("stringify").execute(data);
    }

    public static Value stringifyPretty(Context context, Value data) {
        Value global = context.getBindings("js");
        return global.getMember("JSON").getMember("stringify").execute(data, null, 2);
    }

    public static void logValue(Value jsValue, int indent) {
        jsValue.getMemberKeys().forEach(key -> {
            StringBuilder sb = new StringBuilder();
            IntStream.range(0, indent).forEach(value -> sb.append("  "));
            logger.info("{}{}", sb.toString(), key);
            logValue(jsValue.getMember(key), indent +1);
        });
    }

    public static String getResourceFileAsString(String fileName) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(fileName)) {
            if (is == null) return null;
            try (InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }
}
