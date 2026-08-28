package com.sentinel.revenue.planning;

import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.model.RecoveryActionStatus;
import com.sentinel.revenue.model.RecoveryStrategy;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.policy.PolicyRuleResult;

import java.util.List;
import java.util.UUID;

public record RecoveryPlanningResult(UUID incidentId, UUID planId, UUID actionId,
                                     RecoveryStrategy strategy, PolicyDecision policyDecision,
                                     RecoveryActionStatus actionStatus,
                                     RevenueIncidentStatus incidentStatus,
                                     List<PolicyRuleResult> ruleTrace, String reason) {
    public RecoveryPlanningResult { ruleTrace = List.copyOf(ruleTrace); }
}
