package com.sentinel.revenue.model;

public enum RecoveryLifecycleStage {
    PROPOSED,
    POLICY_APPROVED,
    EXECUTION_REQUESTED,
    PROVIDER_ACCEPTED,
    AWAITING_RECONCILIATION,
    RECOVERED_CONFIRMED,
    FAILED_CONFIRMED,
    EXPIRED
}
