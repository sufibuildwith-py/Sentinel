package com.sentinel.revenue.policy;

import com.sentinel.revenue.model.PolicyDecision;

import java.util.List;

public record PolicyEvaluation(PolicyDecision decision, List<PolicyRuleResult> rules,
                               String reason) {
    public PolicyEvaluation { rules = List.copyOf(rules); }
}
