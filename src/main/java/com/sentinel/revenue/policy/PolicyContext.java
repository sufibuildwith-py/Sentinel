package com.sentinel.revenue.policy;

import com.sentinel.revenue.model.RecoveryStrategy;

import java.time.Instant;
import java.util.Set;

public record PolicyContext(
        double confidence,
        long amountMinor,
        Set<String> paymentStatuses,
        boolean existingActiveRecovery,
        int retryCount,
        int maximumCustomerActionCount,
        RecoveryStrategy strategy,
        boolean paymentAlreadyRecovered,
        Instant actionExpiresAt,
        Instant evaluatedAt,
        int attemptCount,
        double riskScore,
        boolean duplicateChargeRisk) {
    public PolicyContext { paymentStatuses = Set.copyOf(paymentStatuses); }
}
