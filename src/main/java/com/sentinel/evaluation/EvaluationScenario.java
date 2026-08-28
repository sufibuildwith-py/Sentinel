package com.sentinel.evaluation;

import com.sentinel.revenue.api.PaymentEventRequest;
import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.model.RecoveryStrategy;

import java.util.List;
import java.util.Set;

public record EvaluationScenario(
        String scenarioId,
        EvaluationCategory category,
        List<PaymentEventRequest> paymentEvents,
        boolean incidentExpected,
        String expectedRootCauseCategory,
        Set<RecoveryStrategy> eligibleStrategies,
        PolicyDecision expectedPolicyDecision,
        boolean approvalRequired,
        String expectedExecutionBehavior,
        String expectedProviderOutcome,
        long expectedFinancialMutationMinor,
        List<String> expectedAuditEvents) {
    public EvaluationScenario {
        paymentEvents = List.copyOf(paymentEvents);
        eligibleStrategies = Set.copyOf(eligibleStrategies);
        expectedAuditEvents = List.copyOf(expectedAuditEvents);
    }
}
