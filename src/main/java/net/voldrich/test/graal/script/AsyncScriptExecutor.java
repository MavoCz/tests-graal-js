package net.voldrich.test.graal.script;

import lombok.extern.slf4j.Slf4j;
import net.voldrich.test.graal.api.ScriptExecutionException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.time.ZoneId;
import java.util.function.Consumer;

import static net.voldrich.test.graal.script.ScriptUtils.stringifyToString;

@Component
@Slf4j
public class AsyncScriptExecutor {

    public static final String JS_LANGUAGE_TYPE = "js";

    private static final Engine engine = Engine.create();

    private static final ScriptSchedulers scriptSchedulers = new ScriptSchedulers();

    public Mono<String> runScript(String script, Consumer<ContextWrapper> contextDecorator) {
        return runScript(parseScript(script), contextDecorator);
    }

    public Mono<String> runScript(Source source, Consumer<ContextWrapper> contextDecorator) {
        Scheduler scheduler = scriptSchedulers.getNextScheduler();
        return Mono.using(
                () -> createNewContext(source, contextDecorator, scheduler),
                this::evaluateAndExecuteScript,
                contextWrapper -> closeContext(contextWrapper, scheduler)
        ).subscribeOn(scheduler);
    }

    public Source parseScript(String script) {
        try {
            return Source.newBuilder(JS_LANGUAGE_TYPE, script, "script")
                    .cached(true)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse script", e);
        }
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
                .map(value -> stringifyToString(contextWrapper.getContext(), value));
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

    private ContextWrapper createNewContext(Source source,
                                            Consumer<ContextWrapper> contextDecorator,
                                            Scheduler scheduler) {
        Context.Builder contextBuilder = Context.newBuilder(JS_LANGUAGE_TYPE)
                .engine(engine)
                .timeZone(ZoneId.of("UTC"));

        Context context = contextBuilder.build();
        ContextWrapper contextWrapper = new ContextWrapper(context, source, scheduler);
        contextDecorator.accept(contextWrapper);
        return contextWrapper;
    }

    private void closeContext(ContextWrapper contextWrapper, Scheduler scheduler) {
        // this looks strange, if we would call it immediately then it would result in failed Promise due to context
        // being closed while evaluating the Promise handler.
        // AsyncScriptExecutor.wrapMonoInPromise subscribe call which invokes promise handler basically bubbles to
        // using.close operator, which closes the context which is evaluating the promise belonging to that context.
        scheduler.schedule(() -> contextWrapper.close(false));
    }
}
