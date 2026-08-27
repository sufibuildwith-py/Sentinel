package com.sentinel.revenue.detection;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record DetectionRuleResult(
        String rule,
        RuleOutcome outcome,
        BigDecimal actualValue,
        String comparison,
        BigDecimal thresholdValue,
        String unit) {

    public String evidenceLine() {
        return "%s %s: actual=%s %s; required %s %s %s".formatted(
                outcome,
                rule,
                display(actualValue),
                unit,
                comparison,
                display(thresholdValue),
                unit);
    }

    private String display(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
