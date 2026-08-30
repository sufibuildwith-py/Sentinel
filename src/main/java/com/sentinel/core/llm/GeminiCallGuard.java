package com.sentinel.core.llm;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@Component
public final class GeminiCallGuard {
    private static final Logger log = LoggerFactory.getLogger(GeminiCallGuard.class);
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "sentinel-gemini-http");
        thread.setDaemon(true);
        return thread;
    });

    public GeminiCallGuard(CircuitBreakerRegistry circuitBreakers, RetryRegistry retries,
                           TimeLimiterRegistry timeLimiters) {
        this.circuitBreaker = circuitBreakers.circuitBreaker("gemini");
        this.retry = retries.retry("gemini");
        this.timeLimiter = timeLimiters.timeLimiter("gemini");
    }

    public HttpResponse<String> send(HttpClient client, HttpRequest request, String operation)
            throws IOException, InterruptedException {
        Callable<HttpResponse<String>> networkCall = () ->
                client.send(request, HttpResponse.BodyHandlers.ofString());
        Callable<HttpResponse<String>> protectedCall = Retry.decorateCallable(retry,
                CircuitBreaker.decorateCallable(circuitBreaker, networkCall));
        try {
            return timeLimiter.executeFutureSupplier(() ->
                    CompletableFuture.supplyAsync(() -> invoke(protectedCall), executor));
        } catch (TimeoutException timeout) {
            log.warn("{} timed out after the configured deadline", operation);
            throw new HttpTimeoutException(operation + " timed out");
        } catch (ExecutionException execution) {
            return rethrow(operation, execution.getCause());
        } catch (CallNotPermittedException open) {
            log.warn("{} rejected because the Gemini circuit is open", operation);
            throw new IOException("Gemini circuit is open", open);
        } catch (Exception failure) {
            return rethrow(operation, failure);
        }
    }

    private HttpResponse<String> invoke(Callable<HttpResponse<String>> call) {
        try {
            return call.call();
        } catch (Exception failure) {
            throw new CompletionException(failure);
        }
    }

    private HttpResponse<String> rethrow(String operation, Throwable failure)
            throws IOException, InterruptedException {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause() : failure;
        if (cause instanceof InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw interrupted;
        }
        if (cause instanceof HttpTimeoutException timeout) {
            log.warn("{} timed out", operation);
            throw timeout;
        }
        if (cause instanceof IOException io) {
            log.warn("{} failed after bounded retries", operation);
            throw io;
        }
        if (cause instanceof CallNotPermittedException open) {
            log.warn("{} rejected because the Gemini circuit is open", operation);
            throw new IOException("Gemini circuit is open", open);
        }
        throw new IOException("Gemini call failed", cause);
    }

    @PreDestroy
    public void close() {
        executor.shutdownNow();
    }
}
