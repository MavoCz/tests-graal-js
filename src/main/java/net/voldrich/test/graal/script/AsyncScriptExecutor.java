package net.voldrich.test.graal.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Scheduler;

import java.util.function.Supplier;

@Component
@Slf4j
public class AsyncScriptExecutor {

    public static final String JS_LANGUAGE_TYPE = "js";

    private static final ScriptSchedulers scriptSchedulers = new ScriptSchedulers();

    public Mono<String> runScript(Source source, Supplier<Context> contextSupplier) {
        Scheduler scheduler = scriptSchedulers.getNextScheduler();
        return Mono.using(() -> createNewContext(source, contextSupplier, scheduler),
                this::evaluateAndExecuteScript,
                this::closeContext
        ).subscribeOn(scheduler);
    }

    private Mono<String> evaluateAndExecuteScript(ContextWrapper contextWrapper) {
        log.info("Evaluating script");
        Value response = contextWrapper.eval();

        if (response.canExecute()) {
            response = response.execute();
        }

        return resolvePromise(response)
                .map(value -> ScriptUtils.stringify(contextWrapper.getContext(), value).toString());
    }

    public Value wrapMonoInPromise(Context context, Mono<?> operation) {
        return ScriptUtils.constructPromise(context).newInstance((ProxyExecutable) arguments -> {
            Value resolve = arguments[0];
            Value reject = arguments[1];

            // Operation result needs to be published on a thread that executed the script.
            // This ensures that one particular script code is always executed on the same thread
            // and we don't need to manage context.enter and context.leave.
            operation.publishOn(scriptSchedulers.getSchedulerBoundToContext(context))
                    .subscribe(resolve::execute, reject::execute);
            return null;
        });
    }

    private Mono<Object> resolvePromise(Object response) {
        if (response instanceof Value) {
            Value promise = (Value) response;
            if (promise.getMetaObject().getMetaSimpleName().equals("Promise")) {
                return Mono.create(sink -> {
                    PromiseConsumer<Object> resolve = new PromiseConsumer<>(sink::success);
                    PromiseConsumer<Object> reject = new PromiseConsumer<>(error -> handleError(sink, error));

                    promise.invokeMember("then", resolve);
                    promise.invokeMember("catch", reject);
                });
            }
        }
        return Mono.just(response);
    }

    private void handleError(MonoSink<Object> sink, Object error) {
        if (error instanceof Throwable) {
            sink.error(((Throwable) error));
        } else if (error instanceof Value) {
            sink.error(((Value) error).throwException());
        } else {
            sink.error(new UnsupportedOperationException("Unknown exception type"));
        }
    }

    private ContextWrapper createNewContext(Source source, Supplier<Context> contextSupplier, Scheduler scheduler) {
        Context context = contextSupplier.get();
        scriptSchedulers.bindContext(context, scheduler);
        return new ContextWrapper(context, source, scheduler);
    }

    private void closeContext(ContextWrapper contextWrapper) {
        scriptSchedulers.unbindContext(contextWrapper.getContext());
        contextWrapper.close();
    }
}
