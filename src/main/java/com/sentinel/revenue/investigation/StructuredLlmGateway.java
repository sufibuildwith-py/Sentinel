package com.sentinel.revenue.investigation;

import com.sentinel.core.llm.LlmClient;
import com.sentinel.core.llm.Prompt;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class StructuredLlmGateway {
    private final LlmClient client;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "sentinel-root-cause-llm");
        thread.setDaemon(true);
        return thread;
    });

    public StructuredLlmGateway(LlmClient client, InvestigationProperties properties) {
        this.client = client;
        CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.circuitBreakerFailureRate())
                .minimumNumberOfCalls(properties.circuitBreakerMinimumCalls())
                .slidingWindowSize(properties.circuitBreakerWindowSize())
                .waitDurationInOpenState(properties.circuitBreakerOpenDuration())
                .build();
        this.circuitBreaker = CircuitBreaker.of("rootCauseLlm", breakerConfig);
        this.timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(properties.timeout()).cancelRunningFuture(true).build());
    }

    public <T> T generate(Prompt prompt, Class<T> outputType) {
        Callable<T> protectedCall = CircuitBreaker.decorateCallable(circuitBreaker,
                () -> client.generateStructured(prompt, outputType));
        try {
            return timeLimiter.executeFutureSupplier(
                    () -> CompletableFuture.supplyAsync(() -> call(protectedCall), executor));
        } catch (Exception exception) {
            throw new LlmUnavailableException("Structured LLM call failed or timed out", exception);
        }
    }

    private <T> T call(Callable<T> call) {
        try { return call.call(); }
        catch (RuntimeException runtime) { throw runtime; }
        catch (Exception exception) { throw new LlmUnavailableException("Structured LLM call failed", exception); }
    }

    @PreDestroy
    public void close() { executor.shutdownNow(); }

    public static final class LlmUnavailableException extends RuntimeException {
        public LlmUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}
