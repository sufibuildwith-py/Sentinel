package com.sentinel.revenue.execution;

public record RecoveryExecutionEligibility(
        boolean enabled,
        boolean eligible,
        String reasonCode,
        String reason
) { }
