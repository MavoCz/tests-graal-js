package net.voldrich.test.graal.script;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ScriptSchedulers {

    private final List<Scheduler> schedulerList;

    private final int numberOfSchedulers;

    private volatile int nextScheduler = 0;

    public ScriptSchedulers() {
        this(Runtime.getRuntime().availableProcessors());
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
}
