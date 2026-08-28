package com.sentinel.revenue.policy;

import com.sentinel.revenue.detection.RuleOutcome;
import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.model.RecoveryStrategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyEngineTest {
    private final PolicyProperties properties = new PolicyProperties(0.85, 100_000,
            3, 2, 0.70, Duration.ofMinutes(30),
            Set.of(RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, RecoveryStrategy.WAIT_FOR_PROVIDER),
            Set.of("CAPTURED", "AUTHORIZED", "PAID", "REFUNDED"));
    private final PolicyEngine engine = new PolicyEngine(properties, new MandatoryStopEvaluator(properties));

    @Test
    void highConfidenceLowValueFirstAttemptIsAuto() {
        PolicyEvaluation result = engine.evaluate(context(0.91, 50_000, false, false));
        assertThat(result.decision()).isEqualTo(PolicyDecision.AUTO);
        assertThat(result.rules()).allMatch(rule -> rule.outcome() == RuleOutcome.PASS);
        assertThat(result.rules()).extracting(PolicyRuleResult::rule)
                .contains("CONFIDENCE_THRESHOLD", "AMOUNT_THRESHOLD", "STRATEGY_ALLOWED");
    }

    @Test
    void lowConfidenceOrHighValueRequiresHuman() {
        assertThat(engine.evaluate(context(0.60, 50_000, false, false)).decision())
                .isEqualTo(PolicyDecision.HUMAN);
        assertThat(engine.evaluate(context(0.95, 500_000, false, false)).decision())
                .isEqualTo(PolicyDecision.HUMAN);
    }

    @Test
    void alreadyPaidAndDuplicateRiskDenyEvenAtHighConfidence() {
        PolicyEvaluation paid = engine.evaluate(context(0.99, 1_000, true, false));
        PolicyEvaluation duplicate = engine.evaluate(context(0.99, 1_000, false, true));

        assertThat(paid.decision()).isEqualTo(PolicyDecision.DENY);
        assertThat(duplicate.decision()).isEqualTo(PolicyDecision.DENY);
        assertThat(paid.rules()).anyMatch(rule -> rule.rule().equals("PAYMENT_NOT_ALREADY_RECOVERED")
                && rule.outcome() == RuleOutcome.FAIL);
        assertThat(duplicate.rules()).anyMatch(rule -> rule.rule().equals("NO_DUPLICATE_CHARGE_RISK")
                && rule.outcome() == RuleOutcome.FAIL);
        assertThat(paid.rules()).allMatch(PolicyRuleResult::mandatoryStop);
        assertThat(duplicate.rules()).allMatch(PolicyRuleResult::mandatoryStop);
    }

    @Test
    void expiredMaximumAttemptsAndRiskAlwaysDenyBeforeAllowRules() {
        Instant now = Instant.now();
        PolicyContext context = new PolicyContext(0.99, 1_000, Set.of("FAILED"), false,
                0, 0, RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, false,
                now.minusSeconds(1), now, 3, 0.90, false);
        PolicyEvaluation result = engine.evaluate(context);
        assertThat(result.decision()).isEqualTo(PolicyDecision.DENY);
        assertThat(result.rules()).allMatch(PolicyRuleResult::mandatoryStop);
        assertThat(result.rules()).filteredOn(rule -> rule.outcome() == RuleOutcome.FAIL)
                .extracting(PolicyRuleResult::rule)
                .contains("ACTION_NOT_EXPIRED", "MAXIMUM_ATTEMPTS_NOT_REACHED", "RISK_SCORE_ACCEPTABLE");
    }

    private PolicyContext context(double confidence, long amount, boolean recovered, boolean duplicate) {
        Instant now = Instant.now();
        return new PolicyContext(confidence, amount, Set.of("FAILED"), false, 0, 0,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, recovered, now.plusSeconds(300), now,
                1, 0.10, duplicate);
    }
}
