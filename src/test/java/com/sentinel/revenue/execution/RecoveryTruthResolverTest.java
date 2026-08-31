package com.sentinel.revenue.execution;

import com.sentinel.revenue.detection.RuleOutcome;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.policy.PolicyEvaluation;
import com.sentinel.revenue.policy.PolicyRuleResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecoveryTruthResolverTest {
    private final RecoveryTruthResolver resolver = new RecoveryTruthResolver();

    @Test
    void providerAcceptanceNeverBecomesConfirmedRecoveryWithoutReconciliation() {
        Fixture fixture = fixture();
        fixture.action.claim("pay_1", "customer_1", "INR", 10_000, "sntl_ref",
                Instant.now(), Instant.now().plusSeconds(60));
        fixture.action.complete("plink_1", "https://rzp.io/i/test", "created", Instant.now(),
                ExecutionMode.RAZORPAY_TEST_MODE);

        RecoveryTruth truth = resolver.resolve(fixture.action, null);

        assertThat(truth.stage()).isEqualTo(RecoveryLifecycleStage.PROVIDER_ACCEPTED);
        assertThat(truth.awaitingReconciliation()).isTrue();
        assertThat(truth.providerConfirmed()).isFalse();
        assertThat(truth.providerConfirmedAmountMinor()).isZero();
    }

    @Test
    void onlyProviderConfirmedOutcomeReachesRecoveredConfirmed() {
        Fixture fixture = fixture();
        fixture.action.claim("pay_1", "customer_1", "INR", 10_000, "sntl_ref",
                Instant.now(), Instant.now().plusSeconds(60));
        fixture.action.complete("plink_1", "https://rzp.io/i/test", "created", Instant.now(),
                ExecutionMode.RAZORPAY_TEST_MODE);
        fixture.action.recordRecovered("paid");

        RecoveryOutcome unconfirmed = new RecoveryOutcome(fixture.action, fixture.incident,
                RecoveryOutcomeStatus.RECOVERED, 10_000, Instant.now(), null);
        assertThat(resolver.resolve(fixture.action, unconfirmed).stage())
                .isEqualTo(RecoveryLifecycleStage.AWAITING_RECONCILIATION);

        RecoveryOutcome confirmed = RecoveryOutcome.providerConfirmed(fixture.action, fixture.incident,
                RecoveryOutcomeStatus.RECOVERED, 10_000, Instant.now(), "evt_paid",
                "VERIFIED_WEBHOOK");
        RecoveryTruth truth = resolver.resolve(fixture.action, confirmed);
        assertThat(truth.stage()).isEqualTo(RecoveryLifecycleStage.RECOVERED_CONFIRMED);
        assertThat(truth.providerConfirmedAmountMinor()).isEqualTo(10_000);
    }

    private Fixture fixture() {
        RevenueIncident incident = new RevenueIncident("UPI_DEGRADATION", RevenueIncidentStatus.APPROVED,
                "HIGH", 10_000, Instant.now(), List.of("pay_1"), List.of("customer_1"),
                List.of(), null, null);
        ReflectionTestUtils.setField(incident, "incidentId", UUID.randomUUID());
        RecoveryPlan plan = new RecoveryPlan(incident, RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK,
                "test", 1, 10_000, new BigDecimal("0.9000"), 8_000,
                RiskLevel.LOW, Instant.now());
        PolicyEvaluation policy = new PolicyEvaluation(PolicyDecision.AUTO,
                List.of(new PolicyRuleResult("TEST", RuleOutcome.PASS, "true", "==", "true",
                        false, "test")), "test");
        RecoveryAction action = RecoveryAction.fromPersistedPolicy(plan, incident, policy,
                10_000, Instant.now());
        return new Fixture(incident, action);
    }

    private record Fixture(RevenueIncident incident, RecoveryAction action) { }
}
