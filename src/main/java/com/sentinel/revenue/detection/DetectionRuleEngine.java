package com.sentinel.revenue.detection;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class DetectionRuleEngine {

    private final DetectionProperties properties;

    public DetectionRuleEngine(DetectionProperties properties) {
        this.properties = properties;
    }

    public DetectionDecision evaluate(PaymentStatistics statistics) {
        List<DetectionRuleResult> results = List.of(
                minimum("MINIMUM_FAILED_VOLUME",
                        statistics.failedEventCount(), properties.minimumVolume(), "events"),
                minimum("MINIMUM_SUCCESS_RATE_DROP",
                        statistics.successRateDrop(), properties.minimumSuccessRateDrop(), "ratio"),
                minimum("MINIMUM_BASELINE_DEVIATION",
                        statistics.baselineDeviation(), properties.minimumBaselineDeviation(),
                        "standard_deviations"),
                minimum("MINIMUM_AMOUNT_AT_RISK",
                        statistics.amountAtRiskMinor(), properties.minimumAmountAtRiskMinor(),
                        "minor_units"));
        return new DetectionDecision(
                results.stream().allMatch(result -> result.outcome() == RuleOutcome.PASS),
                results);
    }

    private DetectionRuleResult minimum(String name, long actual, long threshold, String unit) {
        return minimum(name, BigDecimal.valueOf(actual), BigDecimal.valueOf(threshold), unit);
    }

    private DetectionRuleResult minimum(String name, double actual, double threshold, String unit) {
        return minimum(name,
                BigDecimal.valueOf(actual).setScale(8, RoundingMode.HALF_UP),
                BigDecimal.valueOf(threshold).setScale(8, RoundingMode.HALF_UP),
                unit);
    }

    private DetectionRuleResult minimum(String name, BigDecimal actual,
                                        BigDecimal threshold, String unit) {
        RuleOutcome outcome = actual.compareTo(threshold) >= 0
                ? RuleOutcome.PASS : RuleOutcome.FAIL;
        return new DetectionRuleResult(name, outcome, actual, ">=", threshold, unit);
    }
}
