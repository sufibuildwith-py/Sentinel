package com.sentinel.revenue.opportunity;

import java.math.BigDecimal;
import java.util.Set;

public record RecoveryActionDefinition(
        OpportunityAction action,
        String version,
        ProviderCapability providerCapability,
        Set<String> eligibleFailureClasses,
        Set<String> eligiblePaymentRails,
        BigDecimal minimumConfidence,
        Long maximumAmountMinor,
        RecoveryRiskClass riskClass,
        String executionAdapter,
        String verificationMethod,
        String compensationStrategy,
        boolean materiallyExecutable) {

    public RecoveryActionDefinition {
        eligibleFailureClasses = Set.copyOf(eligibleFailureClasses);
        eligiblePaymentRails = Set.copyOf(eligiblePaymentRails);
        if (minimumConfidence == null || minimumConfidence.compareTo(BigDecimal.ZERO) < 0
                || minimumConfidence.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("minimumConfidence must be between 0 and 1");
        }
        if (maximumAmountMinor != null && maximumAmountMinor < 0) {
            throw new IllegalArgumentException("maximumAmountMinor cannot be negative");
        }
    }

    public static RecoveryActionDefinition noAction() {
        return new RecoveryActionDefinition(OpportunityAction.NO_ACTION,
                ProviderCapabilityRegistry.CATALOG_VERSION, ProviderCapability.NONE, Set.of("ANY"),
                Set.of("ANY"), BigDecimal.ZERO, null, RecoveryRiskClass.NONE,
                "NoExecutionAdapter", "NO_FINANCIAL_EFFECT", "No compensation required", false);
    }
}
