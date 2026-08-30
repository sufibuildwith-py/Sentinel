package com.sentinel.core.ratelimit;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sentinel.rate-limit")
public record RateLimitProperties(
        @Min(1) int demoRequestsPerMinute,
        @Min(1) int executionRequestsPerMinute,
        @Min(1) int evaluationRequestsPerMinute
) {
}
