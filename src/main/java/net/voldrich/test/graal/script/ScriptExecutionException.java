package net.voldrich.test.graal.script;

import lombok.Getter;

@Getter
public class ScriptExecutionException extends RuntimeException {

    private final String scriptOutput;

    private final String source;

    private final String stack;

    public ScriptExecutionException(ContextWrapper contextWrapper, String message) {
        super(message);
        this.source = "unknown";
        this.stack = "";
        this.scriptOutput = contextWrapper.getScriptOutput();
    }

    public ScriptExecutionException(ContextWrapper contextWrapper, String message, String stack) {
        super(message);
        this.source = "script";
        this.stack = stack;
        this.scriptOutput = contextWrapper.getScriptOutput();
    }

    public ScriptExecutionException(ContextWrapper contextWrapper, Throwable hostException) {
        this(contextWrapper, hostException, "");
    }

    public ScriptExecutionException(ContextWrapper contextWrapper, Throwable hostException, String stack) {
        super(hostException);
        this.source = "host";
        this.stack = stack;
        this.scriptOutput = contextWrapper.getScriptOutput();
    }

}
