package com.sentinel.revenue.policy;

import com.sentinel.revenue.model.RecoveryStrategy;

import java.time.Duration;
import java.util.Set;

public record MerchantPolicyConstitution(Authority authority, Safety safety, Limits limits,
                                         Escalation escalation, Set<RecoveryStrategy> allowedStrategies) {
    public MerchantPolicyConstitution { allowedStrategies = Set.copyOf(allowedStrategies); }
    public record Authority(String llm, String financialExecution) { }
    public record Safety(String ambiguousState, String possibleExistingDebit,
                         String fraudSignal, String staleState) { }
    public record Limits(long maximumAutoAmountMinor, int maximumAttempts,
                         int maximumCustomerContacts, long maximumDailyExposureMinor,
                         Duration actionTtl) { }
    public record Escalation(String highValue, String lowConfidence, String policyConflict) { }
}
