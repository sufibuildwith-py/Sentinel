package com.sentinel.revenue.policy;

import com.sentinel.revenue.detection.RuleOutcome;

public record PolicyRuleResult(String rule, RuleOutcome outcome, String actualValue,
                               String comparison, String thresholdValue,
                               boolean mandatoryStop, String explanation) {
    public String evidenceLine() {
        return "%s %s: actual=%s; required %s %s. %s".formatted(
                outcome, rule, actualValue, comparison, thresholdValue, explanation);
    }
}
