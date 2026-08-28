package com.sentinel.revenue.metrics;

import com.sentinel.revenue.detection.RuleOutcome;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.policy.PolicyEvaluation;
import com.sentinel.revenue.policy.PolicyRuleResult;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RevenueMetricsServiceTest {
    @Test void reconcilesCurrentOutcomeProjectionsWithoutSummingWebhookEvents() {
        RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
        RecoveryOutcomeRepository outcomes = mock(RecoveryOutcomeRepository.class);
        RecoveryPlanRepository plans = mock(RecoveryPlanRepository.class);
        RevenueIncident incident = new RevenueIncident("UPI", RevenueIncidentStatus.RECOVERED, "HIGH",
                50_000, Instant.now(), List.of("p1"), List.of("c1"), List.of(), "UPI down", null);
        ReflectionTestUtils.setField(incident, "incidentId", UUID.randomUUID());
        RecoveryPlan plan = new RecoveryPlan(incident, RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK,
                "test", 1, 10_000, new BigDecimal("0.9"), 8_000, RiskLevel.LOW, Instant.now());
        ReflectionTestUtils.setField(plan, "id", UUID.randomUUID());
        RecoveryAction action = RecoveryAction.fromPersistedPolicy(plan, incident,
                new PolicyEvaluation(PolicyDecision.AUTO, List.of(new PolicyRuleResult("T", RuleOutcome.PASS,
                        "1", "=", "1", false, "test")), "test"), 10_000, Instant.now());
        ReflectionTestUtils.setField(action, "id", UUID.randomUUID());
        action.claim("p1", "c1", "INR", 10_000, "sntl", Instant.now(), Instant.now().plusSeconds(60));
        action.complete("plink_1", "https://rzp.io/i/x", "created", Instant.now());
        RecoveryOutcome current = new RecoveryOutcome(action, incident, RecoveryOutcomeStatus.RECOVERED,
                7_500, Instant.now(), "latest_event");
        when(incidents.findAll()).thenReturn(List.of(incident));
        when(actions.findAll()).thenReturn(List.of(action));
        when(outcomes.findAll()).thenReturn(List.of(current));
        when(plans.findById(plan.getId())).thenReturn(Optional.of(plan));

        RevenueMetrics metrics = new RevenueMetricsService(incidents, actions, outcomes, plans).metrics();
        assertThat(metrics.label()).isEqualTo(RevenueMetricsService.LABEL);
        assertThat(metrics.mode()).isEqualTo("TEST");
        assertThat(metrics.revenueAtRiskMinor()).isEqualTo(50_000);
        assertThat(metrics.attemptedRecoveryMinor()).isEqualTo(10_000);
        assertThat(metrics.recoveredRevenueMinor()).isEqualTo(7_500);
        assertThat(metrics.recoveryRate()).isEqualByComparingTo("0.7500");
        assertThat(metrics.strategyPerformance()).singleElement()
                .satisfies(strategy -> assertThat(strategy.recoveredRevenueMinor()).isEqualTo(7_500));
    }
}
