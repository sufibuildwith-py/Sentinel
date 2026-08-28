package com.sentinel.revenue.execution;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "sentinel.razorpay")
public record RazorpayProperties(
        boolean enabled,
        String keyId,
        String keySecret,
        URI baseUrl,
        Duration connectTimeout,
        Duration requestTimeout,
        Duration actionExpiry,
        Duration linkExpiry,
        int maximumAttempts,
        float circuitBreakerFailureRate,
        int circuitBreakerMinimumCalls,
        int circuitBreakerWindowSize,
        Duration circuitBreakerOpenDuration,
        boolean notificationsEnabled
) {
    public RazorpayProperties {
        keyId = keyId == null ? "" : keyId.trim();
        keySecret = keySecret == null ? "" : keySecret;
        if (keyId.startsWith("rzp_live_") || keySecret.startsWith("rzp_live_")) {
            throw new IllegalArgumentException("Razorpay Live Mode credentials are forbidden");
        }
        if (enabled && (!keyId.startsWith("rzp_test_") || keySecret.isBlank())) {
            throw new IllegalArgumentException("Enabled Razorpay execution requires Test Mode credentials");
        }
        if (maximumAttempts < 1) throw new IllegalArgumentException("maximum-attempts must be positive");
        if (baseUrl == null || connectTimeout == null || requestTimeout == null
                || actionExpiry == null || linkExpiry == null || circuitBreakerOpenDuration == null) {
            throw new IllegalArgumentException("Razorpay durations and base-url are required");
        }
    }

    public URI endpoint(String path) {
        return URI.create(baseUrl.toString().replaceAll("/+$", "") + path);
    }
}
