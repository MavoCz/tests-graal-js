package net.voldrich.test.graal.script;

import lombok.extern.slf4j.Slf4j;
import net.voldrich.test.graal.api.ScriptExecutionException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.function.Supplier;

@Component
@Slf4j
public class AsyncScriptExecutor {

    public static final String JS_LANGUAGE_TYPE = "js";

    private static final ScriptSchedulers scriptSchedulers = new ScriptSchedulers();

    public Mono<String> runScript(Source source, Supplier<Context> contextSupplier) {
        Scheduler scheduler = scriptSchedulers.getNextScheduler();
        return Mono.using(
                () -> createNewContext(source, contextSupplier, scheduler),
                this::evaluateAndExecuteScript,
                contextWrapper -> closeContext(contextWrapper, scheduler)
        ).subscribeOn(scheduler);
    }

    private Mono<String> evaluateAndExecuteScript(ContextWrapper contextWrapper) {
        Mono<Object> functionExecution = Mono.create(sink -> {
            sink.onCancel(contextWrapper::forceClose);
            log.debug("Evaluating script");
            try {
                Value response = contextWrapper.eval();

                if (response.canExecute()) {
                    response = response.execute();
                }

                sink.success(response);
            } catch (Exception e) {
                sink.error(e);
            }
        });

        return functionExecution
                .flatMap(response -> resolvePromise(response, contextWrapper))
                .map(value -> ScriptUtils.stringifyToString(contextWrapper.getContext(), value));
    }

    private Mono<Object> resolvePromise(Object response, ContextWrapper contextWrapper) {
        if (response instanceof Value) {
            Value promise = (Value) response;
            if (promise.getMetaObject().getMetaSimpleName().equals("Promise")) {
                return Mono.create(sink -> {
                    sink.onCancel(contextWrapper::forceClose);
                    PromiseConsumer<Object> resolve = new PromiseConsumer<>(sink::success);
                    PromiseConsumer<Object> reject = new PromiseConsumer<>(error -> sink.error(convertError(error)));

                    promise
                            .invokeMember("then", resolve)
                            .invokeMember("catch", reject);
                });
            }
        }
        return Mono.just(response);
    }

    public Value wrapMonoInPromise(Context context, Mono<?> operation, String description) {
        return ScriptUtils.constructPromise(context).newInstance((ProxyExecutable) arguments -> {
            Value resolve = arguments[0];
            Value reject = arguments[1];

            // Operation result needs to be published on a thread that executed the script.
            // This ensures that one particular script code is always executed on the same thread
            // and we don't need to manage context.enter and context.leave.
            operation.publishOn(scriptSchedulers.getSchedulerBoundToContext(context))
                    .subscribe(
                            result -> executeAndIgnoreClosed(resolve, result, description + " succeeded"),
                            error -> executeAndIgnoreClosed(reject, error, description + " failed"));
            return null;
        });
    }

    private void executeAndIgnoreClosed(Value fnc, Object argument, String msg) {
        try {
            fnc.executeVoid(argument);
        } catch (Exception ex) {
            log.warn("Exception when executing Async operation {} with argument: {}", msg, argument.toString());
        }
    }

    private Throwable convertError(Object error) {
        try {
            if (error instanceof Throwable) {
                // received when error is thrown in host (java) code
                return ((Throwable) error);
            } else {
                // received when error is thrown in JS
                Value errorValue = Value.asValue(error);

                if (errorValue.isException()) {
                    return errorValue.throwException();
                } else if (errorValue.hasMember("stack")) {
                    Value polyglotValue = errorValue.getMember("stack");
                    return new ScriptExecutionException(polyglotValue.toString());
                }
            }

            return new ScriptExecutionException("Unknown exception when converting error");
        } catch (Exception e) {
            return e;
        }
    }

    private ContextWrapper createNewContext(Source source, Supplier<Context> contextSupplier, Scheduler scheduler) {
        Context context = contextSupplier.get();
        scriptSchedulers.bindContext(context, scheduler);
        return new ContextWrapper(context, source, scheduler);
    }

    private void closeContext(ContextWrapper contextWrapper, Scheduler scheduler) {
        scriptSchedulers.unbindContext(contextWrapper.getContext());
        // this looks strange, if we would call it immediately then it would result in failed Promise due to context
        // being closed while evaluating the Promise handler.
        // AsyncScriptExecutor.wrapMonoInPromise subscribe call which invokes promise handler basically bubbles to
        // using.close operator, which closes the context which is evaluating the promise belonging to that context.
        scheduler.schedule(() -> contextWrapper.close(false));
    }
}
