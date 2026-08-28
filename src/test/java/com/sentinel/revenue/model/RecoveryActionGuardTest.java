package com.sentinel.revenue.model;

import com.sentinel.revenue.policy.PolicyEvaluation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecoveryActionGuardTest {
    @Test
    void cannotCreateActionWithoutPersistableTracedPolicyResult() {
        RevenueIncident incident = new RevenueIncident("UPI_DEGRADATION", RevenueIncidentStatus.DIAGNOSED,
                "HIGH", 50_000, Instant.now(), List.of("p1"), List.of("c1"), List.of(), null, null);
        RecoveryPlan plan = new RecoveryPlan(incident, RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK,
                "test", 1, 50_000, new BigDecimal("0.9000"), 40_000,
                RiskLevel.LOW, Instant.now());

        assertThatThrownBy(() -> RecoveryAction.fromPersistedPolicy(plan, incident, null,
                50_000, Instant.now())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RecoveryAction.fromPersistedPolicy(plan, incident,
                new PolicyEvaluation(PolicyDecision.AUTO, List.of(), "untraced"),
                50_000, Instant.now())).isInstanceOf(IllegalArgumentException.class);
    }
}
