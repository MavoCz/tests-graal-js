package net.voldrich.test.graal.script;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
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
        this.numberOfSchedulers = numberOfSchedulers;
        this.schedulerList = createSchedulers(numberOfSchedulers);
    }

    private List<Scheduler> createSchedulers(int numberOfSchedulers) {
        List<Scheduler> list = new ArrayList<>(numberOfSchedulers);
        for (int i = 0; i < numberOfSchedulers; i++) {
            CustomizableThreadFactory tf = new CustomizableThreadFactory();
            tf.setDaemon(true);
            tf.setThreadNamePrefix("Script-" + i + "-");
            list.add(Schedulers.fromExecutorService(Executors.newSingleThreadExecutor(tf)));
        }
        return Collections.unmodifiableList(list);
    }

    public Scheduler getNextScheduler() {
        nextScheduler = (nextScheduler + 1) % numberOfSchedulers;
        return schedulerList.get(nextScheduler);
    }
}
