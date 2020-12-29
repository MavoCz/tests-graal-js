package net.voldrich.test.graal.api;

public class ScriptExecutionException extends RuntimeException {

    public Throwable hostException;

    public String scriptOutput;

    public ScriptExecutionException(String message) {
        super(message);
    }
}
