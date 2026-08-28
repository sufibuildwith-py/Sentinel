package com.sentinel.revenue.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.revenue.audit.AuditLogService;
import com.sentinel.revenue.detection.RuleOutcome;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.policy.PolicyEvaluation;
import com.sentinel.revenue.policy.PolicyRuleResult;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WebhookOutcomeProcessorTest {
    @Test void partialThenPaidUsesLatestCumulativeAmountAndCreatesOneMemoryRecord() {
        Fixture f = fixture();
        AtomicReference<RecoveryOutcome> current = new AtomicReference<>();
        when(f.outcomes.findByRecoveryActionId(f.action.getId())).thenAnswer(call -> Optional.ofNullable(current.get()));
        when(f.outcomes.saveAndFlush(any())).thenAnswer(call -> { current.set(call.getArgument(0)); return current.get(); });

        assertThat(f.processor.process("evt_partial", payload("payment_link.partially_paid", "partially_paid", 4_000), "d1").disposition())
                .isEqualTo("APPLIED");
        assertThat(current.get().getRecoveredAmountMinor()).isEqualTo(4_000);
        assertThat(f.incident.getStatus()).isEqualTo(RevenueIncidentStatus.MONITORING);

        assertThat(f.processor.process("evt_paid", payload("payment_link.paid", "paid", 10_000), "d2").disposition())
                .isEqualTo("APPLIED");
        assertThat(current.get().getRecoveredAmountMinor()).isEqualTo(10_000);
        assertThat(current.get().getStatus()).isEqualTo(RecoveryOutcomeStatus.RECOVERED);
        assertThat(f.incident.getStatus()).isEqualTo(RevenueIncidentStatus.RECOVERED);
        verify(f.history, times(1)).saveAndFlush(any(HistoricalIncident.class));

        assertThat(f.processor.process("evt_cancel_late", payload("payment_link.cancelled", "cancelled", 10_000), "d3").disposition())
                .isEqualTo("IGNORED_STALE");
        assertThat(current.get().getStatus()).isEqualTo(RecoveryOutcomeStatus.RECOVERED);
    }

    @Test void cancellationCanBeSupersededByOutOfOrderPaidButNotByPartial() {
        Fixture f = fixture(); AtomicReference<RecoveryOutcome> current = new AtomicReference<>();
        when(f.outcomes.findByRecoveryActionId(f.action.getId())).thenAnswer(call -> Optional.ofNullable(current.get()));
        when(f.outcomes.saveAndFlush(any())).thenAnswer(call -> { current.set(call.getArgument(0)); return current.get(); });
        assertThat(f.processor.process("evt_cancel", payload("payment_link.cancelled", "cancelled", 0), "d1").disposition())
                .isEqualTo("APPLIED");
        assertThat(f.incident.getStatus()).isEqualTo(RevenueIncidentStatus.STOPPED);
        assertThat(f.processor.process("evt_partial_late", payload("payment_link.partially_paid", "partially_paid", 2_000), "d2").disposition())
                .isEqualTo("IGNORED_STALE");
        assertThat(f.processor.process("evt_paid_late", payload("payment_link.paid", "paid", 10_000), "d3").disposition())
                .isEqualTo("APPLIED");
        assertThat(f.incident.getStatus()).isEqualTo(RevenueIncidentStatus.RECOVERED);
        assertThat(current.get().getRecoveredAmountMinor()).isEqualTo(10_000);
    }

    @Test void unknownLinkMalformedAndMismatchedMoneyNeverMutateRevenue() {
        Fixture unknown = fixture();
        when(unknown.actions.findForWebhookByExternalResourceId(anyString())).thenReturn(Optional.empty());
        assertThat(unknown.processor.process("evt_unknown", payload("payment_link.paid", "paid", 10_000), "d").disposition())
                .isEqualTo("IGNORED");
        verifyNoInteractions(unknown.outcomes);

        Fixture mismatch = fixture();
        assertThat(mismatch.processor.process("evt_bad_amount",
                payloadWithMoney("payment_link.paid", "paid", 11_000, 10_000, "INR"), "d").disposition())
                .isEqualTo("REJECTED");
        assertThat(mismatch.processor.process("evt_bad_currency",
                payloadWithMoney("payment_link.paid", "paid", 10_000, 10_000, "USD"), "d").disposition())
                .isEqualTo("REJECTED");
        verify(mismatch.outcomes, never()).saveAndFlush(any());

        Fixture malformed = fixture();
        assertThat(malformed.processor.process("evt_malformed", "{".getBytes(StandardCharsets.UTF_8), "d").disposition())
                .isEqualTo("REJECTED");
        verifyNoInteractions(malformed.actions);
    }

    private Fixture fixture() {
        ProcessedWebhookEventRepository webhooks = mock(ProcessedWebhookEventRepository.class);
        RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
        RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        RecoveryOutcomeRepository outcomes = mock(RecoveryOutcomeRepository.class);
        RecoveryPlanRepository plans = mock(RecoveryPlanRepository.class);
        HistoricalIncidentRepository history = mock(HistoricalIncidentRepository.class);
        AuditLogService audit = mock(AuditLogService.class);
        WebhookSecurityAuditService security = mock(WebhookSecurityAuditService.class);
        when(webhooks.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        RevenueIncident incident = new RevenueIncident("UPI_DEGRADATION", RevenueIncidentStatus.MONITORING,
                "HIGH", 20_000, Instant.now(), List.of("p1"), List.of("c1"), List.of(), "UPI issuer degradation", null);
        ReflectionTestUtils.setField(incident, "incidentId", UUID.randomUUID());
        RecoveryPlan plan = new RecoveryPlan(incident, RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK,
                "alternative", 1, 10_000, new BigDecimal("0.9000"), 8_000, RiskLevel.LOW, Instant.now());
        ReflectionTestUtils.setField(plan, "id", UUID.randomUUID());
        PolicyRuleResult trace = new PolicyRuleResult("TEST", RuleOutcome.PASS, "true", "==", "true", false, "test");
        RecoveryAction action = RecoveryAction.fromPersistedPolicy(plan, incident,
                new PolicyEvaluation(PolicyDecision.AUTO, List.of(trace), "test"), 10_000, Instant.now());
        ReflectionTestUtils.setField(action, "id", UUID.randomUUID());
        action.claim("p1", "c1", "INR", 10_000, "sntl_ref", Instant.now(), Instant.now().plusSeconds(60));
        action.complete("plink_1", "https://rzp.io/i/test", "created", Instant.now());
        when(actions.findForWebhookByExternalResourceId("plink_1")).thenReturn(Optional.of(action));
        when(actions.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(incidents.findById(incident.getIncidentId())).thenReturn(Optional.of(incident));
        when(incidents.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(plans.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(history.existsByOriginalIncidentIncidentId(incident.getIncidentId())).thenReturn(false);
        WebhookOutcomeProcessor processor = new WebhookOutcomeProcessor(webhooks, actions, incidents,
                outcomes, plans, history, new ObjectMapper(), audit, security);
        return new Fixture(processor, action, incident, webhooks, actions, outcomes, history);
    }
    private byte[] payload(String event, String status, long paid) { return payloadWithMoney(event, status, 10_000, paid, "INR"); }
    private byte[] payloadWithMoney(String event, String status, long amount, long paid, String currency) {
        return ("{\"event\":\"" + event + "\",\"payload\":{\"payment_link\":{\"entity\":{" +
                "\"id\":\"plink_1\",\"amount\":" + amount + ",\"amount_paid\":" + paid +
                ",\"currency\":\"" + currency + "\",\"status\":\"" + status + "\"}}}}")
                .getBytes(StandardCharsets.UTF_8);
    }
    private record Fixture(WebhookOutcomeProcessor processor, RecoveryAction action, RevenueIncident incident,
                           ProcessedWebhookEventRepository webhooks, RecoveryActionRepository actions,
                           RecoveryOutcomeRepository outcomes, HistoricalIncidentRepository history) { }
}
