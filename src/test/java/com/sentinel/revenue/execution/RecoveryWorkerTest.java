package com.sentinel.revenue.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.revenue.model.ProviderOrder;
import com.sentinel.revenue.model.ProviderPayment;
import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RecoveryActionStatus;
import com.sentinel.revenue.model.RecoveryJob;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.repository.ProviderOrderRepository;
import com.sentinel.revenue.repository.ProviderPaymentRepository;
import com.sentinel.revenue.repository.RecoveryActionRepository;
import com.sentinel.revenue.repository.RecoveryOutcomeRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import com.sentinel.revenue.service.RecoveryJobService;
import com.sentinel.revenue.service.WebhookEventService;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecoveryWorkerTest {

    @Test
    void fixturePaymentLinkRunsOnceAndMarksJobSucceeded() {
        Fixture fixture = new Fixture();
        when(fixture.orders.findByIdempotencyKey("recovery-" + fixture.incidentId))
                .thenReturn(Optional.empty());
        when(fixture.razorpay.createPaymentLink(fixture.incidentId, 4_299,
                "Sentinel recovery for interrupted payment", "recovery-" + fixture.incidentId))
                .thenReturn(new JSONObject().put("id", "plink_fixture"));

        fixture.worker.processJob(fixture.jobId);

        verify(fixture.razorpay).createPaymentLink(fixture.incidentId, 4_299,
                "Sentinel recovery for interrupted payment", "recovery-" + fixture.incidentId);
        verify(fixture.jobs).markSucceeded(fixture.jobId);
        verify(fixture.jobs, never()).markFailed(any(), anyString());
    }

    @Test
    void providerFailureReturnsJobToDurableRetryLifecycle() {
        Fixture fixture = new Fixture();
        when(fixture.orders.findByIdempotencyKey("recovery-" + fixture.incidentId))
                .thenReturn(Optional.empty());
        when(fixture.razorpay.createPaymentLink(any(), anyLong(), anyString(), anyString()))
                .thenThrow(new RazorpayFailure(RazorpayFailure.Kind.TEMPORARY, "HTTP_500"));

        fixture.worker.processJob(fixture.jobId);

        verify(fixture.jobs).markFailed(fixture.jobId,
                "Razorpay request failed (HTTP_500)");
        verify(fixture.jobs, never()).markSucceeded(fixture.jobId);
    }

    @Test
    void paidProviderOrderPreventsASecondProviderExecution() {
        Fixture fixture = new Fixture();
        ProviderOrder paid = new ProviderOrder(fixture.incidentId, "plink_paid", 4_299,
                "INR", "PAID", "https://rzp.io/i/existing",
                "recovery-" + fixture.incidentId);
        RevenueIncident incident = mock(RevenueIncident.class);
        when(incident.getStatus()).thenReturn(RevenueIncidentStatus.MONITORING);
        when(fixture.orders.findByIdempotencyKey("recovery-" + fixture.incidentId))
                .thenReturn(Optional.of(paid));
        when(fixture.payments.findFirstByRazorpayOrderIdAndStatusIgnoreCase("plink_paid", "CAPTURED"))
                .thenReturn(Optional.of(new ProviderPayment(null, "pay_captured", "plink_paid",
                        "CAPTURED", 4_299L, "upi", Instant.now(), "{}")));
        when(fixture.incidents.findById(fixture.incidentId)).thenReturn(Optional.of(incident));
        when(fixture.outcomes.findByRecoveryActionId(fixture.actionId)).thenReturn(Optional.empty());

        fixture.worker.processJob(fixture.jobId);

        verify(fixture.razorpay, never()).createPaymentLink(any(), anyLong(), anyString(), anyString());
        verify(fixture.executions, never()).execute(any());
        verify(fixture.jobs).markSucceeded(fixture.jobId);
        verify(incident).transitionTo(RevenueIncidentStatus.RECOVERED);
    }

    @Test
    void paidOrderWithoutCapturedProviderPaymentDoesNotFabricateRecovery() {
        Fixture fixture = new Fixture();
        ProviderOrder paid = new ProviderOrder(fixture.incidentId, "plink_paid", 4_299,
                "INR", "PAID", "https://rzp.io/i/existing",
                "recovery-" + fixture.incidentId);
        when(fixture.orders.findByIdempotencyKey("recovery-" + fixture.incidentId))
                .thenReturn(Optional.of(paid));
        when(fixture.payments.findFirstByRazorpayOrderIdAndStatusIgnoreCase("plink_paid", "CAPTURED"))
                .thenReturn(Optional.empty());

        fixture.worker.processJob(fixture.jobId);

        verify(fixture.razorpay, never()).createPaymentLink(any(), anyLong(), anyString(), anyString());
        verify(fixture.outcomes, never()).saveAndFlush(any());
        verify(fixture.incidents, never()).saveAndFlush(any());
        verify(fixture.jobs).markSucceeded(fixture.jobId);
    }

    private static final class Fixture {
        private final UUID jobId = UUID.randomUUID();
        private final UUID incidentId = UUID.randomUUID();
        private final UUID actionId = UUID.randomUUID();
        private final RecoveryJobService jobs = mock(RecoveryJobService.class);
        private final RecoveryExecutionService executions = mock(RecoveryExecutionService.class);
        private final RazorpayAdapter razorpay = mock(RazorpayAdapter.class);
        private final ProviderOrderRepository orders = mock(ProviderOrderRepository.class);
        private final ProviderPaymentRepository payments = mock(ProviderPaymentRepository.class);
        private final RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
        private final RecoveryOutcomeRepository outcomes = mock(RecoveryOutcomeRepository.class);
        private final RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        private final WebhookEventService events = mock(WebhookEventService.class);
        private final RecoveryJob job = mock(RecoveryJob.class);
        private final RecoveryAction action = mock(RecoveryAction.class);
        private final RecoveryWorker worker;

        private Fixture() {
            when(job.getId()).thenReturn(jobId);
            when(job.getIncidentId()).thenReturn(incidentId);
            when(job.getPolicyDecisionId()).thenReturn(actionId);
            when(job.getStrategy()).thenReturn("PAYMENT_LINK");
            when(job.getStatus()).thenReturn(RecoveryJob.RUNNING);
            when(jobs.markRunning(jobId)).thenReturn(job);
            when(jobs.get(jobId)).thenReturn(job);
            when(actions.findById(actionId)).thenReturn(Optional.of(action));
            when(action.getIncidentId()).thenReturn(incidentId);
            when(action.getStatus()).thenReturn(RecoveryActionStatus.AUTO_APPROVED);
            when(action.getAmountMinor()).thenReturn(4_299L);
            when(action.getCurrency()).thenReturn("INR");
            RazorpayProperties properties = new RazorpayProperties(false, "", "",
                    URI.create("https://api.razorpay.com"), Duration.ofSeconds(1),
                    Duration.ofSeconds(2), Duration.ofMinutes(30), Duration.ofHours(24),
                    3, 50, 3, 3, Duration.ofSeconds(30), false);
            worker = new RecoveryWorker(jobs, executions, razorpay, properties, orders, payments,
                    actions, outcomes, incidents, events, new ObjectMapper());
        }
    }
}
