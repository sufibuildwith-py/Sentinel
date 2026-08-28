package com.sentinel.revenue.planning;

import com.sentinel.core.agent.*;
import com.sentinel.revenue.audit.AuditLogService;
import com.sentinel.revenue.detection.RuleOutcome;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.policy.*;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecoveryPlanningServiceTest {
    @Test
    void persistsTracedPolicyDecisionBeforeCreatingAutoApprovedAction() {
        RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        RecoveryPlanRepository plans = mock(RecoveryPlanRepository.class);
        RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
        RecoveryOutcomeRepository outcomes = mock(RecoveryOutcomeRepository.class);
        PaymentEventRepository payments = mock(PaymentEventRepository.class);
        RecoveryPlannerAgent planner = mock(RecoveryPlannerAgent.class);
        PolicyEngine policyEngine = mock(PolicyEngine.class);
        AuditLogService audit = mock(AuditLogService.class);
        PolicyProperties properties = new PolicyProperties(0.85, 100_000, 3, 2, 0.7,
                Duration.ofMinutes(30), Set.of(RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK),
                Set.of("PAID", "REFUNDED", "CAPTURED", "AUTHORIZED"));

        UUID incidentId = UUID.randomUUID();
        RevenueIncident incident = new RevenueIncident("UPI_DEGRADATION", RevenueIncidentStatus.DIAGNOSED,
                "LOW", 50_000, Instant.now(), List.of("p1"), List.of("c1"), List.of(), null, null);
        ReflectionTestUtils.setField(incident, "incidentId", incidentId);
        RecoveryPlan plan = new RecoveryPlan(incident, RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK,
                "UPI route degraded", 1, 50_000, new BigDecimal("0.9500"), 45_000,
                RiskLevel.LOW, Instant.now());
        AgentResult<RecoveryPlan> agentResult = new AgentResult<>("RecoveryPlannerAgent", "proposal",
                new Confidence(0.95), List.of(new Evidence("root-cause", "UPI degraded", Instant.now(), Map.of())),
                List.of(), Instant.now(), Instant.now(), AgentStatus.SUCCEEDED, plan);
        PolicyRuleResult rule = new PolicyRuleResult("CONFIDENCE_THRESHOLD", RuleOutcome.PASS,
                "0.95", ">=", "0.85", false, "passed");
        PolicyEvaluation policy = new PolicyEvaluation(PolicyDecision.AUTO, List.of(rule), "all passed");

        when(incidents.findById(incidentId)).thenReturn(Optional.of(incident));
        when(planner.execute(eq(incident), any())).thenReturn(agentResult);
        when(plans.saveAndFlush(plan)).thenAnswer(invocation -> {
            ReflectionTestUtils.setField(plan, "id", UUID.randomUUID()); return plan;
        });
        when(policyEngine.evaluate(any())).thenReturn(policy);
        when(payments.findAllByPaymentIdIn(any())).thenReturn(List.of(new PaymentEvent(
                "p1", "o1", "c1", 50_000, "INR", "UPI", "HDFC", "FAILED",
                "UPI_ISSUER_UNAVAILABLE", "failure", Instant.now(), 1, null, 0, null, Map.of())));
        when(outcomes.findAllByIncidentIncidentId(incidentId)).thenReturn(List.of());
        when(actions.findAllByIncidentIncidentIdAndStatusIn(any(), any())).thenReturn(List.of());
        when(actions.findAll()).thenReturn(List.of());
        when(actions.saveAndFlush(any())).thenAnswer(invocation -> {
            RecoveryAction action = invocation.getArgument(0);
            ReflectionTestUtils.setField(action, "id", UUID.randomUUID()); return action;
        });

        RecoveryPlanningService service = new RecoveryPlanningService(incidents, plans, actions,
                outcomes, payments, planner, policyEngine, properties, audit);
        RecoveryPlanningResult result = service.plan(incidentId);

        assertThat(result.policyDecision()).isEqualTo(PolicyDecision.AUTO);
        assertThat(result.actionStatus()).isEqualTo(RecoveryActionStatus.AUTO_APPROVED);
        assertThat(result.incidentStatus()).isEqualTo(RevenueIncidentStatus.APPROVED);
        InOrder order = inOrder(audit, actions);
        order.verify(audit).append(eq(incident), eq("POLICY_ENGINE"), isNull(),
                eq("POLICY_DECISION"), anyList(), any(), anyString(), anyList(), eq("AUTO"),
                isNull(), isNull(), eq("AUTO"));
        order.verify(actions).saveAndFlush(any(RecoveryAction.class));
    }
}
