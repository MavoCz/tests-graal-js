package net.voldrich.test.graal.script;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import reactor.core.scheduler.Scheduler;


public class ContextWrapper implements AutoCloseable {

    private final Context context;
    private final Source source;
    private final Scheduler scheduler;
    private boolean closed = false;

    public ContextWrapper(Context context, Source source, Scheduler scheduler) {
        this.context = context;
        this.source = source;
        this.scheduler = scheduler;
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
        if (!closed) {
            this.closed = true;
            this.context.close(true);
        }
    }


}
