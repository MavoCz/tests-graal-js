package net.voldrich.test.graal.dto;

import lombok.Getter;
import net.voldrich.graal.async.script.ScriptExecutionException;

@Getter
public class ScriptErrorResponseDto {

    public String output;

    public String message;

    public String source;

    public String stack;

    public ScriptErrorResponseDto(ScriptExecutionException exception) {
        this.output = exception.getScriptOutput();
        this.message = exception.getMessage();
        this.source = exception.getSource();
        this.stack = exception.getStack();
    }
}
