package com.sentinel.revenue.investigation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "sentinel.investigation")
public record InvestigationProperties(
        @NotNull Duration timeout,
        @Min(1) int memoryTopK,
        @DecimalMin("-1.0") @DecimalMax("1.0") double memoryMinimumSimilarity,
        @DecimalMin("1.0") @DecimalMax("100.0") float circuitBreakerFailureRate,
        @Min(1) int circuitBreakerMinimumCalls,
        @Min(1) int circuitBreakerWindowSize,
        @NotNull Duration circuitBreakerOpenDuration) {
}
