package com.sentinel.revenue.model;

public enum RecoveryStrategy {
    ALTERNATIVE_PAYMENT_LINK,
    DEFERRED_RETRY,
    RECOVERY_REMINDER,
    WAIT_FOR_PROVIDER,
    HUMAN_ESCALATION,
    NO_ACTION
}
