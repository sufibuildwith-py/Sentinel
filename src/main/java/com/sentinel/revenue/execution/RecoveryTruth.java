package com.sentinel.revenue.execution;

import com.sentinel.revenue.model.ExecutionMode;
import com.sentinel.revenue.model.RecoveryLifecycleStage;

public record RecoveryTruth(
        RecoveryLifecycleStage stage,
        ExecutionMode executionMode,
        boolean providerAccepted,
        boolean awaitingReconciliation,
        boolean providerConfirmed,
        long providerConfirmedAmountMinor,
        String basis
) {
    public RecoveryTruth {
        if (providerConfirmedAmountMinor < 0) {
            throw new IllegalArgumentException("Provider-confirmed amount cannot be negative");
        }
        if (!providerConfirmed && providerConfirmedAmountMinor != 0) {
            throw new IllegalArgumentException("Unconfirmed recovery cannot carry confirmed value");
        }
    }
}
