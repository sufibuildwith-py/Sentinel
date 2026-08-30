package com.sentinel.revenue.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RecoveryJob;
import com.sentinel.revenue.model.RecoveryStrategy;
import com.sentinel.revenue.model.WebhookEvent;
import com.sentinel.revenue.repository.ProviderOrderRepository;
import com.sentinel.revenue.repository.RecoveryActionRepository;
import com.sentinel.revenue.repository.RecoveryPlanRepository;
import com.sentinel.revenue.service.RecoveryJobService;
import com.sentinel.revenue.service.WebhookEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class RecoveryJobEnqueuer {

    private static final Logger log = LoggerFactory.getLogger(RecoveryJobEnqueuer.class);
    private final RecoveryJobService jobs;
    private final WebhookEventService events;
    private final ProviderOrderRepository providerOrders;
    private final RecoveryActionRepository actions;
    private final RecoveryPlanRepository plans;

    public RecoveryJobEnqueuer(RecoveryJobService jobs, WebhookEventService events,
                               ProviderOrderRepository providerOrders,
                               RecoveryActionRepository actions,
                               RecoveryPlanRepository plans) {
        this.jobs = jobs;
        this.events = events;
        this.providerOrders = providerOrders;
        this.actions = actions;
        this.plans = plans;
    }

    public Optional<RecoveryJob> enqueue(WebhookEvent event, JsonNode payload) {
        if (!"payment.failed".equals(event.getEventType())
                && !"payment.captured".equals(event.getEventType())) {
            return Optional.empty();
        }
        Optional<UUID> incidentId = incidentId(payload);
        if (incidentId.isEmpty()) {
            log.debug("Webhook event_id={} has no Sentinel incident mapping", event.getEventId());
            return Optional.empty();
        }
        events.associateIncident(event.getId(), incidentId.get());
        Optional<RecoveryAction> action = action(payload, incidentId.get());
        String strategy = "payment.captured".equals(event.getEventType())
                ? "RECONCILE_CAPTURED" : strategy(action);
        UUID persistedPolicyAction = action.map(RecoveryAction::getId).orElse(null);
        return Optional.of(jobs.createJobIfAbsent(
                incidentId.get(), persistedPolicyAction, strategy));
    }

    private Optional<UUID> incidentId(JsonNode root) {
        for (JsonNode candidate : new JsonNode[]{
                root.path("incident_id"),
                root.path("payload").path("payment").path("entity").path("notes").path("sentinel_incident"),
                root.path("payload").path("order").path("entity").path("notes").path("sentinel_incident"),
                root.path("payload").path("payment_link").path("entity").path("notes").path("sentinel_incident")}) {
            Optional<UUID> parsed = uuid(candidate.asText());
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        String orderId = root.path("payload").path("payment").path("entity")
                .path("order_id").asText();
        if (!orderId.isBlank()) {
            return providerOrders.findByRazorpayOrderId(orderId)
                    .map(com.sentinel.revenue.model.ProviderOrder::getIncidentId);
        }
        return Optional.empty();
    }

    private Optional<RecoveryAction> action(JsonNode root, UUID incidentId) {
        String note = root.path("payload").path("payment").path("entity")
                .path("notes").path("sentinel_action").asText();
        Optional<UUID> actionId = uuid(note);
        if (actionId.isPresent()) {
            Optional<RecoveryAction> found = actions.findById(actionId.get());
            if (found.isPresent()) {
                return found;
            }
        }
        return actions.findFirstByIncidentIncidentIdOrderByCreatedAtDesc(incidentId);
    }

    private String strategy(Optional<RecoveryAction> action) {
        if (action.isEmpty()) {
            return "MANUAL";
        }
        RecoveryStrategy strategy = plans.findById(action.get().getRecoveryPlanId())
                .map(plan -> plan.getStrategy()).orElse(RecoveryStrategy.HUMAN_ESCALATION);
        return switch (strategy) {
            case ALTERNATIVE_PAYMENT_LINK -> "PAYMENT_LINK";
            case DEFERRED_RETRY -> "RETRY";
            default -> "MANUAL";
        };
    }

    private Optional<UUID> uuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }
}
