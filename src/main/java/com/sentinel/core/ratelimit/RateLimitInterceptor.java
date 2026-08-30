package com.sentinel.core.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public final class RateLimitInterceptor implements HandlerInterceptor {
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);
    private final RateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitInterceptor(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Limit limit = limitFor(request);
        if (limit == null) {
            return true;
        }

        String key = limit.scope() + ':' + clientAddress(request);
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> bucket(limit.requestsPerMinute()));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfter = Math.max(1, (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0));
            throw new RateLimitExceededException(retryAfter);
        }
        return true;
    }

    private Limit limitFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI().toLowerCase(Locale.ROOT);
        if (path.startsWith("/api/v1/demo/")) {
            return new Limit("demo", properties.demoRequestsPerMinute());
        }
        if (path.startsWith("/api/v1/revenue/") && path.endsWith("/execute")) {
            return new Limit("execute", properties.executionRequestsPerMinute());
        }
        if (path.equals("/api/v1/evaluation/run")) {
            return new Limit("evaluation", properties.evaluationRequestsPerMinute());
        }
        if (path.equals("/api/v1/webhooks/razorpay")) {
            return new Limit("razorpay-webhook", properties.webhookRequestsPerMinute());
        }
        return null;
    }

    private String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String candidate = forwarded.split(",", 2)[0].trim();
            if (candidate.matches("[0-9A-Fa-f:.]{1,64}")) {
                return candidate;
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }

    private Bucket bucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, REFILL_PERIOD)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private record Limit(String scope, int requestsPerMinute) {
    }
}
