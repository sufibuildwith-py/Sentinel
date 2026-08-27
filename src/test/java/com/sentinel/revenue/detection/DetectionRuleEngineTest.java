package com.sentinel.revenue.detection;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DetectionRuleEngineTest {

    private final DetectionRuleEngine engine = new DetectionRuleEngine(properties());

    @Test
    void reportsEveryPassingRuleWithActualAndThreshold() {
        DetectionDecision decision = engine.evaluate(statistics(24, 240_000, 0.20, 15.0));

        assertThat(decision.incidentRequired()).isTrue();
        assertThat(decision.rules()).hasSize(4).allSatisfy(result -> {
            assertThat(result.outcome()).isEqualTo(RuleOutcome.PASS);
            assertThat(result.evidenceLine())
                    .contains("actual=")
                    .contains("required >=");
        });
    }

    @Test
    void rejectsWholeClusterWhenAnyThresholdFailsWithoutHidingOtherResults() {
        DetectionDecision decision = engine.evaluate(statistics(6, 90_000, 0.05, 1.0));

        assertThat(decision.incidentRequired()).isFalse();
        assertThat(decision.rules()).hasSize(4);
        assertThat(decision.rules())
                .filteredOn(result -> result.outcome() == RuleOutcome.FAIL)
                .extracting(DetectionRuleResult::rule)
                .containsExactlyInAnyOrder(
                        "MINIMUM_FAILED_VOLUME", "MINIMUM_SUCCESS_RATE_DROP",
                        "MINIMUM_BASELINE_DEVIATION", "MINIMUM_AMOUNT_AT_RISK");
    }

    private PaymentStatistics statistics(int failures, long amountAtRiskMinor,
                                         double successRateDrop, double deviation) {
        double baseline = 0.95;
        return new PaymentStatistics(
                failures, failures, amountAtRiskMinor, amountAtRiskMinor,
                baseline - successRateDrop, Map.of(), Map.of(), Map.of(),
                0.0, 0.0, 0.0, baseline, 0.05, deviation, 1);
    }

    private DetectionProperties properties() {
        return new DetectionProperties(
                Duration.ofHours(1), Duration.ofHours(1), Duration.ofHours(24),
                Duration.ofMinutes(15), 10, 0.20, 2.0, 100_000,
                0.95, 0.05, Set.of("CAPTURED", "AUTHORIZED"), "merchantId");
    }
}
