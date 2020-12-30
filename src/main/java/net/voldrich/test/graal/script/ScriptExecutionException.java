package net.voldrich.test.graal.script;

import lombok.Getter;
import net.voldrich.test.graal.script.ContextWrapper;

@Getter
public class ScriptExecutionException extends RuntimeException {

    private final String scriptOutput;

    private final String source;

    private final String stack;

    public ScriptExecutionException(ContextWrapper contextWrapper, String message, String stack) {
        super(message);
        this.source = "script";
        this.stack = stack;
        this.scriptOutput = contextWrapper.getScriptOutput();
    }

    public ScriptExecutionException(ContextWrapper contextWrapper, Throwable hostException) {
        super(hostException);
        this.source = "host";
        this.stack = "";
        this.scriptOutput = contextWrapper.getScriptOutput();
    }

    public ScriptExecutionException(ContextWrapper contextWrapper, String message) {
        super(message);
        this.source = "unknown";
        this.stack = "";
        this.scriptOutput = contextWrapper.getScriptOutput();
    }
}
