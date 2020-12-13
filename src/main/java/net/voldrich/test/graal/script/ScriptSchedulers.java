package net.voldrich.test.graal.script;

import org.graalvm.polyglot.Context;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class ScriptSchedulers {

    private final List<Scheduler> schedulerList;

    private final int numberOfSchedulers;

    private final ConcurrentHashMap<Context, Scheduler> contextToSchedulerMap = new ConcurrentHashMap<>();

    private volatile int nextScheduler = 0;

    public ScriptSchedulers() {
        this(Runtime.getRuntime().availableProcessors() * 2);
    }

    public ScriptSchedulers(int numberOfSchedulers) {
        schedulerList = new ArrayList<>(numberOfSchedulers);
        this.numberOfSchedulers = numberOfSchedulers;
        for (int i = 0; i < numberOfSchedulers; i++) {
            CustomizableThreadFactory tf = new CustomizableThreadFactory();
            tf.setDaemon(true);
            tf.setThreadNamePrefix("Script-" + i + "-");
            schedulerList.add(Schedulers.fromExecutorService(Executors.newSingleThreadExecutor(tf)));
        }
    }

    public Scheduler getNextScheduler() {
        nextScheduler = (nextScheduler + 1) % numberOfSchedulers;
        return schedulerList.get(nextScheduler);
    }

    public void bindContext(Context context, Scheduler scheduler) {
        contextToSchedulerMap.put(context, scheduler);
    }

    public void unbindContext(Context context) {
        contextToSchedulerMap.remove(context);
    }

    public Scheduler getSchedulerBoundToContext(Context context) {
        return contextToSchedulerMap.get(context);
    }
}
