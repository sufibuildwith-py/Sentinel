package com.sentinel.revenue.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RecoveryJob;
import com.sentinel.revenue.model.RecoveryPlan;
import com.sentinel.revenue.model.RecoveryStrategy;
import com.sentinel.revenue.model.WebhookEvent;
import com.sentinel.revenue.repository.ProviderOrderRepository;
import com.sentinel.revenue.repository.RecoveryActionRepository;
import com.sentinel.revenue.repository.RecoveryPlanRepository;
import com.sentinel.revenue.service.RecoveryJobService;
import com.sentinel.revenue.service.WebhookEventService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecoveryJobEnqueuerTest {

    @Test
    void failedPaymentMapsPersistedPolicyActionToPaymentLinkJob() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID actionId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        RecoveryJobService jobs = mock(RecoveryJobService.class);
        WebhookEventService events = mock(WebhookEventService.class);
        ProviderOrderRepository orders = mock(ProviderOrderRepository.class);
        RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
        RecoveryPlanRepository plans = mock(RecoveryPlanRepository.class);
        RecoveryAction action = mock(RecoveryAction.class);
        RecoveryPlan plan = mock(RecoveryPlan.class);
        RecoveryJob job = mock(RecoveryJob.class);
        WebhookEvent event = mock(WebhookEvent.class);
        when(event.getId()).thenReturn(eventId);
        when(event.getEventId()).thenReturn("evt_failed_1");
        when(event.getEventType()).thenReturn("payment.failed");
        when(actions.findById(actionId)).thenReturn(Optional.of(action));
        when(action.getId()).thenReturn(actionId);
        when(action.getRecoveryPlanId()).thenReturn(planId);
        when(plans.findById(planId)).thenReturn(Optional.of(plan));
        when(plan.getStrategy()).thenReturn(RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK);
        when(jobs.createJobIfAbsent(incidentId, actionId, "PAYMENT_LINK")).thenReturn(job);
        JsonNode payload = new ObjectMapper().readTree("""
                {"event":"payment.failed","payload":{"payment":{"entity":{"notes":{
                  "sentinel_incident":"%s","sentinel_action":"%s"}}}}}
                """.formatted(incidentId, actionId));

        Optional<RecoveryJob> result = new RecoveryJobEnqueuer(
                jobs, events, orders, actions, plans).enqueue(event, payload);

        assertThat(result).contains(job);
        verify(events).associateIncident(eventId, incidentId);
        verify(jobs).createJobIfAbsent(incidentId, actionId, "PAYMENT_LINK");
    }
}
