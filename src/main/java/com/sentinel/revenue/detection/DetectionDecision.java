package com.sentinel.revenue.detection;

import java.util.List;

public record DetectionDecision(
        boolean incidentRequired,
        List<DetectionRuleResult> rules) {

    public DetectionDecision {
        rules = List.copyOf(rules);
    }
}
