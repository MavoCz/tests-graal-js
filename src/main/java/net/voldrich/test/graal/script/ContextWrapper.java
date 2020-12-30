package net.voldrich.test.graal.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

@Slf4j
public class ContextWrapper implements AutoCloseable {

    private final Context context;
    private final Source source;
    private final Scheduler scheduler;
    private ByteArrayOutputStream scriptOutputStream;
    private boolean closed = false;

    public ContextWrapper(Context context, Source source, Scheduler scheduler, ByteArrayOutputStream outputStream) {
        this.context = context;
        this.source = source;
        this.scheduler = scheduler;
        this.scriptOutputStream = outputStream;
    }

    public Context getContext() {
        return context;
    }

    public boolean isClosed() {
        return closed;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Value eval() {
        return context.eval(source);
    }

    @Override
    public void close() {
        this.close(false);
    }

    public void forceClose() {
        this.close(true);
    }

    public Value wrapMonoInPromise(Mono<?> operation, String description) {
        String currentJsStack = ScriptUtils.getCurrentJsStack(context);
        return ScriptUtils.getGlobalPromise(context).newInstance((ProxyExecutable) arguments -> {
            Value resolve = arguments[0];
            Value reject = arguments[1];

            // Operation result needs to be published on a thread that executed the script.
            // This ensures that one particular script code is always executed on the same thread
            // and we don't need to manage context.enter and context.leave.
            operation.publishOn(scheduler)
                    .subscribe(
                            resolve::executeVoid,
                            error -> addStackToException(reject, error, description + "\n" + currentJsStack));
            return null;
        });
    }

    private void addStackToException(Value fnc, Object argument, String stack) {
        if (argument instanceof Throwable) {
            argument = new ScriptExecutionException(this, (Throwable) argument, stack);
        }

        fnc.executeVoid(argument);
    }

    public void close(boolean force) {
        if (!closed) {
            log.debug("Closing context, force: {}", force);
            this.closed = true;
            this.context.close(force);
        }
    }


    public String getScriptOutput() {
        return scriptOutputStream.toString(Charset.defaultCharset());
    }
}
