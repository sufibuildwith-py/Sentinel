package com.sentinel.revenue.execution;

public class RecoveryExecutionUnavailableException extends IllegalStateException {
    private final String reasonCode;

    public RecoveryExecutionUnavailableException(RecoveryExecutionEligibility eligibility) {
        super(eligibility.reason());
        this.reasonCode = eligibility.reasonCode();
    }

    public String getReasonCode() {
        return reasonCode;
    }
}
