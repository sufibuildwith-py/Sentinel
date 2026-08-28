package com.sentinel.revenue.policy;

import com.sentinel.revenue.model.RecoveryStrategy;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "sentinel.policy")
public record PolicyProperties(
        @DecimalMin("0.0") @DecimalMax("1.0") double autoConfidenceThreshold,
        @Min(0) long maximumAutoAmountMinor,
        @Min(1) int maximumAttempts,
        @Min(1) int perCustomerActionLimit,
        @DecimalMin("0.0") @DecimalMax("1.0") double maximumRiskScore,
        @NotNull Duration actionTtl,
        @NotEmpty Set<RecoveryStrategy> allowedStrategies,
        @NotEmpty Set<String> paidOrRefundedStatuses) {
    public PolicyProperties {
        allowedStrategies = allowedStrategies == null ? Set.of() : Set.copyOf(allowedStrategies);
        paidOrRefundedStatuses = paidOrRefundedStatuses == null ? Set.of() : Set.copyOf(paidOrRefundedStatuses);
        if (actionTtl != null && (actionTtl.isZero() || actionTtl.isNegative())) {
            throw new IllegalArgumentException("sentinel.policy.action-ttl must be positive");
        }
    }
}
