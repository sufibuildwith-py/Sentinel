package com.sentinel.revenue.execution;

import com.sentinel.revenue.audit.AuditLogService;
import com.sentinel.revenue.detection.RuleOutcome;
import com.sentinel.revenue.governor.ExecutionEnvelope;
import com.sentinel.revenue.governor.GovernorEvaluation;
import com.sentinel.revenue.governor.RecoverySafetyGovernor;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.policy.PolicyEvaluation;
import com.sentinel.revenue.policy.PolicyRuleResult;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecoveryExecutionServiceTest {
    @Test void autoExecutionTargetsOneExactPaymentAndIsSequentiallyIdempotent() {
        Fixture f = fixture(PolicyDecision.AUTO, RecoveryActionStatus.AUTO_APPROVED,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "FAILED", Instant.now());
        when(f.gateway.findPaymentLinkByReference(anyString())).thenReturn(Optional.empty());
        when(f.gateway.createPaymentLink(any())).thenReturn(link("plink_1"));

        RecoveryExecutionResponse first = f.service.execute(f.incidentId);
        RecoveryExecutionResponse second = f.service.execute(f.incidentId);

        assertThat(first.mode()).isEqualTo("TEST");
        assertThat(first.providerId()).isEqualTo("plink_1");
        assertThat(first.referenceId()).hasSize(37).startsWith("sntl_");
        assertThat(first.actionStatus()).isEqualTo(RecoveryActionStatus.EXECUTED);
        assertThat(second.existing()).isTrue();
        assertThat(f.action.getAmountMinor()).isEqualTo(12_345);
        assertThat(f.action.getTargetPaymentId()).isEqualTo("local_failed_1");
        verify(f.gateway, times(1)).createPaymentLink(argThat(command -> command.amountMinor() == 12_345
                && command.hideUpi() && !command.notificationsEnabled()));
    }

    @Test void approvedHumanActionCanExecute() {
        Fixture f = fixture(PolicyDecision.HUMAN, RecoveryActionStatus.APPROVED,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "FAILED", Instant.now());
        when(f.gateway.findPaymentLinkByReference(anyString())).thenReturn(Optional.empty());
        when(f.gateway.createPaymentLink(any())).thenReturn(link("plink_human"));
        assertThat(f.service.execute(f.incidentId).providerId()).isEqualTo("plink_human");
        verify(f.governor).evaluate(eq(f.action), eq(RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK), eq(12_345L), any());
        verify(f.gateway, times(1)).createPaymentLink(any());
    }

    @Test void shadowAndOpportunitySubsystemsCannotOverrideAuthoritativeExecutionGates() {
        assertThat(List.of(RecoveryExecutionService.class.getDeclaredFields()))
                .extracting(field -> field.getType().getPackageName())
                .noneMatch(packageName -> packageName.contains(".opportunity")
                        || packageName.contains(".replay") || packageName.contains(".economics"));

        Fixture allowed = fixture(PolicyDecision.AUTO, RecoveryActionStatus.AUTO_APPROVED,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "FAILED", Instant.now());
        when(allowed.gateway.findPaymentLinkByReference(anyString())).thenReturn(Optional.empty());
        when(allowed.gateway.createPaymentLink(any())).thenReturn(link("plink_authoritative"));
        assertThat(allowed.service.execute(allowed.incidentId).providerId()).isEqualTo("plink_authoritative");
        verify(allowed.gateway, times(1)).createPaymentLink(any());

        Fixture denied = fixture(PolicyDecision.AUTO, RecoveryActionStatus.AUTO_APPROVED,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "FAILED", Instant.now());
        when(denied.governor.evaluate(any(), any(), anyLong(), any())).thenReturn(
                new GovernorEvaluation(UUID.randomUUID(), false, 0, envelope(), List.of("KILL_SWITCH")));
        assertThat(denied.service.execute(denied.incidentId).actionStatus()).isEqualTo(RecoveryActionStatus.STOPPED);
        verifyNoInteractions(denied.gateway);
    }

    @Test void governorDenialIsPersistedAndReturnsWithoutCallingProvider() {
        Fixture denied = fixture(PolicyDecision.HUMAN, RecoveryActionStatus.APPROVED,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "FAILED", Instant.now());
        when(denied.governor.evaluate(any(), any(), anyLong(), any())).thenReturn(
                new GovernorEvaluation(UUID.randomUUID(), false, 0, envelope(),
                        List.of("MAX_UNRECONCILED_VALUE actual=50000")));

        RecoveryExecutionResponse response = denied.service.execute(denied.incidentId);

        assertThat(response.actionStatus()).isEqualTo(RecoveryActionStatus.STOPPED);
        assertThat(response.message()).contains("governor denied").contains("no provider call");
        assertThat(denied.action.getStatus()).isEqualTo(RecoveryActionStatus.STOPPED);
        verifyNoInteractions(denied.gateway);
        verify(denied.audit).append(eq(denied.incident), eq("GOVERNOR"), isNull(),
                eq("BLAST_RADIUS_EVALUATED"), anyList(), isNull(),
                eq("Execution envelope denied"), eq(List.of()), eq("DENY"),
                isNull(), isNull(), eq("No provider call"));
    }

    @Test void ambiguousCreateReconcilesWithoutSecondCreate() {
        Fixture f = fixture(PolicyDecision.AUTO, RecoveryActionStatus.AUTO_APPROVED,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "FAILED", Instant.now());
        when(f.gateway.findPaymentLinkByReference(anyString()))
                .thenReturn(Optional.empty(), Optional.of(link("plink_recovered")));
        when(f.gateway.createPaymentLink(any())).thenThrow(
                new RazorpayFailure(RazorpayFailure.Kind.AMBIGUOUS, "TIMEOUT"));
        assertThat(f.service.execute(f.incidentId).providerId()).isEqualTo("plink_recovered");
        verify(f.gateway, times(1)).createPaymentLink(any());
    }

    @Test void nonRetryableReferenceLookupFailsSafelyInsteadOfLoopingRetryPending() {
        Fixture f = fixture(PolicyDecision.AUTO, RecoveryActionStatus.AUTO_APPROVED,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "FAILED", Instant.now());
        when(f.gateway.findPaymentLinkByReference(anyString())).thenThrow(
                new RazorpayFailure(RazorpayFailure.Kind.NON_RETRYABLE, "HTTP_400"));

        RecoveryExecutionResponse response = f.service.execute(f.incidentId);

        assertThat(response.actionStatus()).isEqualTo(RecoveryActionStatus.FAILED);
        assertThat(f.action.getStatus()).isEqualTo(RecoveryActionStatus.FAILED);
        assertThat(f.incident.getStatus()).isEqualTo(RevenueIncidentStatus.FAILED);
        verify(f.gateway, never()).createPaymentLink(any());
    }

    @Test void paidExpiredWrongStrategyAndUngatedActionsNeverCallProvider() {
        Fixture paid = fixture(PolicyDecision.AUTO, RecoveryActionStatus.AUTO_APPROVED,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "CAPTURED", Instant.now());
        assertThat(paid.service.execute(paid.incidentId).actionStatus()).isEqualTo(RecoveryActionStatus.STOPPED);
        verifyNoInteractions(paid.gateway);

        Fixture expired = fixture(PolicyDecision.AUTO, RecoveryActionStatus.AUTO_APPROVED,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "FAILED", Instant.now().minus(Duration.ofHours(2)));
        assertThat(expired.service.execute(expired.incidentId).actionStatus()).isEqualTo(RecoveryActionStatus.STOPPED);
        verifyNoInteractions(expired.gateway);

        Fixture wrong = fixture(PolicyDecision.AUTO, RecoveryActionStatus.AUTO_APPROVED,
                RecoveryStrategy.DEFERRED_RETRY, "FAILED", Instant.now());
        assertThatThrownBy(() -> wrong.service.execute(wrong.incidentId)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(wrong.gateway);

        for (RecoveryActionStatus status : List.of(RecoveryActionStatus.PENDING_APPROVAL,
                RecoveryActionStatus.REJECTED, RecoveryActionStatus.STOPPED)) {
            Fixture gated = fixture(PolicyDecision.HUMAN, status,
                    RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "FAILED", Instant.now());
            assertThatThrownBy(() -> gated.service.execute(gated.incidentId)).isInstanceOf(IllegalStateException.class);
            verifyNoInteractions(gated.gateway);
        }
    }

    @Test void providerBackedAlreadyPaidAndMaximumAttemptsStopBeforeCreate() {
        Fixture providerPaid = fixture(PolicyDecision.AUTO, RecoveryActionStatus.AUTO_APPROVED,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "FAILED", Instant.now());
        when(providerPaid.payments.findAllByPaymentIdIn(any())).thenReturn(List.of(new PaymentEvent(
                "pay_provider_1", "order_1", "customer_0182", 12_345, "INR", "UPI", "Bank X",
                "FAILED", "UPI_DOWN", "failed", Instant.now(), 1, null, 0, null, Map.of())));
        when(providerPaid.gateway.fetchPayment("pay_provider_1"))
                .thenReturn(new ProviderPayment("pay_provider_1", "captured", 12_345, "INR"));
        assertThat(providerPaid.service.execute(providerPaid.incidentId).actionStatus())
                .isEqualTo(RecoveryActionStatus.STOPPED);
        verify(providerPaid.gateway, never()).createPaymentLink(any());

        Fixture exhausted = fixture(PolicyDecision.AUTO, RecoveryActionStatus.AUTO_APPROVED,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "FAILED", Instant.now());
        ReflectionTestUtils.setField(exhausted.action, "executionAttempts", 3);
        assertThat(exhausted.service.execute(exhausted.incidentId).actionStatus())
                .isEqualTo(RecoveryActionStatus.STOPPED);
        verifyNoInteractions(exhausted.gateway);
    }

    private Fixture fixture(PolicyDecision decision, RecoveryActionStatus desiredStatus,
                            RecoveryStrategy strategy, String paymentStatus, Instant createdAt) {
        RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
        RecoveryPlanRepository plans = mock(RecoveryPlanRepository.class);
        RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        PaymentEventRepository payments = mock(PaymentEventRepository.class);
        RazorpayGateway gateway = mock(RazorpayGateway.class);
        AuditLogService audit = mock(AuditLogService.class);
        RecoverySafetyGovernor governor = mock(RecoverySafetyGovernor.class);
        UUID incidentId = UUID.randomUUID();
        RevenueIncident incident = new RevenueIncident("UPI_DEGRADATION", RevenueIncidentStatus.APPROVED,
                "LOW", 50_000, Instant.now(), List.of("local_failed_1", "outside_2"),
                List.of("customer_0182"), List.of(), null, null);
        ReflectionTestUtils.setField(incident, "incidentId", incidentId);
        RecoveryPlan plan = new RecoveryPlan(incident, strategy, "UPI degraded", 2, 50_000,
                new BigDecimal("0.9500"), 40_000, RiskLevel.LOW, createdAt);
        ReflectionTestUtils.setField(plan, "id", UUID.randomUUID());
        PolicyRuleResult trace = new PolicyRuleResult("TEST", RuleOutcome.PASS, "true", "==", "true", false, "test");
        RecoveryAction action = RecoveryAction.fromPersistedPolicy(plan, incident,
                new PolicyEvaluation(decision, List.of(trace), "test"), 50_000, createdAt);
        ReflectionTestUtils.setField(action, "id", UUID.randomUUID());
        if (decision == PolicyDecision.HUMAN && desiredStatus == RecoveryActionStatus.APPROVED) action.approve(Instant.now());
        else if (desiredStatus != action.getStatus()) ReflectionTestUtils.setField(action, "status", desiredStatus);
        PaymentEvent payment = new PaymentEvent("local_failed_1", "order_1", "customer_0182", 12_345,
                "INR", "UPI", "Bank X", paymentStatus, "UPI_DOWN", "failed", Instant.now(),
                1, null, 0, null, Map.of());
        when(actions.findForExecutionByIncidentId(incidentId)).thenReturn(Optional.of(action));
        when(actions.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(plans.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(incidents.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidents.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(payments.findAllByPaymentIdIn(any())).thenReturn(List.of(payment));
        RazorpayProperties properties = properties();
        when(governor.evaluate(any(), any(), anyLong(), any())).thenReturn(
                new GovernorEvaluation(UUID.randomUUID(), true, 50_000, envelope(), List.of()));
        return new Fixture(new RecoveryExecutionService(actions, plans, incidents, payments,
                gateway, properties, audit, governor), incidentId, incident, action, gateway,
                payments, audit, governor);
    }

    private RazorpayProperties properties() {
        return new RazorpayProperties(true, "rzp_test_key", "secret-value", URI.create("http://localhost"),
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofMinutes(30), Duration.ofHours(24),
                3, 50, 2, 4, Duration.ofSeconds(30), false);
    }
    private PaymentLinkResource link(String id) { return new PaymentLinkResource(id, "sntl_ref", "https://rzp.io/i/test", "created"); }
    private ExecutionEnvelope envelope() {
        return new ExecutionEnvelope(1_000_000, 10, 100_000, 10, 10, 3, 4, 0.5, 500_000);
    }
    private record Fixture(RecoveryExecutionService service, UUID incidentId,
                           RevenueIncident incident,
                           RecoveryAction action, RazorpayGateway gateway,
                           PaymentEventRepository payments, AuditLogService audit,
                           RecoverySafetyGovernor governor) { }
}
