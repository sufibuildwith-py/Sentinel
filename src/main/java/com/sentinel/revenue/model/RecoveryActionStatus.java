package com.sentinel.revenue.model;

public enum RecoveryActionStatus {
    PROPOSED,
    AUTO_APPROVED,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    EXECUTING,
    RETRY_PENDING,
    EXECUTION_UNCERTAIN,
    EXECUTED,
    PARTIALLY_RECOVERED,
    RECOVERED,
    CANCELLED,
    FAILED,
    STOPPED
}
