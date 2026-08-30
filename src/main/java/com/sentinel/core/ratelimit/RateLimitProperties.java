package com.sentinel.core.ratelimit;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sentinel.rate-limit")
public record RateLimitProperties(
        @Min(1) int demoRequestsPerMinute,
        @Min(1) int executionRequestsPerMinute,
        @Min(1) int evaluationRequestsPerMinute,
        @Min(1) int webhookRequestsPerMinute
) {
    @ConstructorBinding
    public RateLimitProperties(int demoRequestsPerMinute,
                               int executionRequestsPerMinute,
                               int evaluationRequestsPerMinute,
                               int webhookRequestsPerMinute) {
        this.demoRequestsPerMinute = demoRequestsPerMinute;
        this.executionRequestsPerMinute = executionRequestsPerMinute;
        this.evaluationRequestsPerMinute = evaluationRequestsPerMinute;
        this.webhookRequestsPerMinute = webhookRequestsPerMinute;
    }

    public RateLimitProperties(int demoRequestsPerMinute, int executionRequestsPerMinute,
                               int evaluationRequestsPerMinute) {
        this(demoRequestsPerMinute, executionRequestsPerMinute,
                evaluationRequestsPerMinute, 60);
    }
}
