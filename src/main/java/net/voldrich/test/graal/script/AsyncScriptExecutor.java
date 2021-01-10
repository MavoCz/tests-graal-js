package net.voldrich.test.graal.script;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.function.Consumer;

import static net.voldrich.test.graal.script.ScriptUtils.stringifyToString;


@Slf4j
public class AsyncScriptExecutor {

    public static final String JS_LANGUAGE_TYPE = "js";

    private final Engine engine;

    private final ScriptSchedulers scriptSchedulers;

    private AsyncScriptExecutor(Builder builder) {
        this.engine = builder.getEngine() != null
                ? builder.getEngine()
                : Engine.create();
        this.scriptSchedulers = builder.getScriptSchedulers() != null
                ? builder.getScriptSchedulers()
                : new ScriptSchedulers();
    }

    public Mono<String> executeScript(String script, Consumer<ScriptContext> contextDecorator) {
        return executeScript(parseScript(script), contextDecorator);
    }

    public Mono<String> executeScript(Source source, Consumer<ScriptContext> contextDecorator) {
        Scheduler scheduler = scriptSchedulers.getNextScheduler();
        return Mono.using(
                () -> createNewContext(source, contextDecorator, scheduler),
                this::evaluateAndExecuteScript,
                this::closeContext
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
                sink.error(convertError(e, contextWrapper));
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
                    try {
                        PromiseConsumer<Object> resolve = new PromiseConsumer<>(sink::success);
                        PromiseConsumer<Object> reject = new PromiseConsumer<>(error ->
                                sink.error(convertError(error, contextWrapper)));

                        promise
                                .invokeMember("then", resolve)
                                .invokeMember("catch", reject);
                    } catch (Exception ex) {
                        sink.error(convertError(ex, contextWrapper));
                    }
                });
            }
        }
        return Mono.just(response);
    }

    private ScriptExecutionException convertError(Object error, ContextWrapper contextWrapper) {
        try {
            if (error instanceof ScriptExecutionException) {
                return (ScriptExecutionException) error;
            } else if (error instanceof Throwable) {
                // received when error is thrown in host (java) code
                return new ScriptExecutionException(contextWrapper, (Throwable) error);
            } else {
                // received when error is thrown in JS
                Value errorValue = Value.asValue(error);
                if (errorValue.isException()) {
                    return new ScriptExecutionException(contextWrapper, errorValue.throwException());
                } else if (errorValue.hasMember("stack")) {
                    Value polyglotValue = errorValue.getMember("stack");
                    return new ScriptExecutionException(contextWrapper, errorValue.toString(), polyglotValue.toString());
                }
            }

            return new ScriptExecutionException(contextWrapper, "Unknown exception when converting error");
        } catch (Exception e) {
            return new ScriptExecutionException(contextWrapper, e);
        }
    }

    private ContextWrapper createNewContext(Source source,
                                            Consumer<ScriptContext> contextDecorator,
                                            Scheduler scheduler) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Context.Builder contextBuilder = Context.newBuilder(JS_LANGUAGE_TYPE)
                .engine(engine)
                .out(outputStream)
                .err(outputStream)
                .timeZone(ZoneId.of("UTC"));

        Context context = contextBuilder.build();
        ContextWrapper contextWrapper = new ContextWrapper(context, source, scheduler, outputStream);
        contextDecorator.accept(contextWrapper);
        return contextWrapper;
    }

    private void closeContext(ContextWrapper contextWrapper) {
        // this looks strange, if we would call it immediately then it would result in failed Promise due to context
        // being closed while evaluating the Promise handler.
        // AsyncScriptExecutor.wrapMonoInPromise subscribe call which invokes promise handler basically bubbles to
        // using.close operator, which closes the context which is evaluating the promise belonging to that context.
        contextWrapper.getScheduler().schedule(contextWrapper::close);
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Builder {
        private Engine engine;

        private ScriptSchedulers scriptSchedulers;

        public AsyncScriptExecutor build() {
            return new AsyncScriptExecutor(this);
        }
    }
}
