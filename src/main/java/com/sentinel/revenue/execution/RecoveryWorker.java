package com.sentinel.revenue.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.core.observability.RequestContext;
import com.sentinel.revenue.model.ProviderOrder;
import com.sentinel.revenue.model.ProviderPayment;
import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RecoveryActionStatus;
import com.sentinel.revenue.model.RecoveryJob;
import com.sentinel.revenue.model.RecoveryOutcome;
import com.sentinel.revenue.model.RecoveryOutcomeStatus;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.model.WebhookEvent;
import com.sentinel.revenue.repository.ProviderOrderRepository;
import com.sentinel.revenue.repository.ProviderPaymentRepository;
import com.sentinel.revenue.repository.RecoveryActionRepository;
import com.sentinel.revenue.repository.RecoveryOutcomeRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import com.sentinel.revenue.service.RecoveryJobService;
import com.sentinel.revenue.service.RevenueIncidentStateMachine;
import com.sentinel.revenue.service.WebhookEventService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "sentinel.recovery.worker", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class RecoveryWorker {

    private static final Logger log = LoggerFactory.getLogger(RecoveryWorker.class);
    private static final Set<String> PAID = Set.of("PAID", "CAPTURED");

    private final RecoveryJobService jobs;
    private final RecoveryExecutionService executions;
    private final RazorpayAdapter razorpay;
    private final RazorpayProperties razorpayProperties;
    private final ProviderOrderRepository providerOrders;
    private final ProviderPaymentRepository providerPayments;
    private final RecoveryActionRepository actions;
    private final RecoveryOutcomeRepository outcomes;
    private final RevenueIncidentRepository incidents;
    private final WebhookEventService webhookEvents;
    private final ObjectMapper json;
    private final RevenueIncidentStateMachine stateMachine = new RevenueIncidentStateMachine();

    public RecoveryWorker(RecoveryJobService jobs,
                          RecoveryExecutionService executions,
                          RazorpayAdapter razorpay,
                          RazorpayProperties razorpayProperties,
                          ProviderOrderRepository providerOrders,
                          ProviderPaymentRepository providerPayments,
                          RecoveryActionRepository actions,
                          RecoveryOutcomeRepository outcomes,
                          RevenueIncidentRepository incidents,
                          WebhookEventService webhookEvents,
                          ObjectMapper json) {
        this.jobs = jobs;
        this.executions = executions;
        this.razorpay = razorpay;
        this.razorpayProperties = razorpayProperties;
        this.providerOrders = providerOrders;
        this.providerPayments = providerPayments;
        this.actions = actions;
        this.outcomes = outcomes;
        this.incidents = incidents;
        this.webhookEvents = webhookEvents;
        this.json = json;
    }

    @Scheduled(fixedDelayString = "${sentinel.recovery.worker.fixed-delay:30s}")
    public void processRecoveryJobs() {
        for (RecoveryJob job : jobs.findPendingDueJobs()) {
            processJob(job.getId());
        }
    }

    public void processJob(UUID jobId) {
        RecoveryJob running = null;
        try {
            running = jobs.markRunning(jobId);
            executeRecovery(running);
            jobs.markSucceeded(jobId);
        } catch (Exception failure) {
            if (running != null) {
                RecoveryJob current = jobs.get(running.getId());
                if (RecoveryJob.RUNNING.equals(current.getStatus())) {
                    jobs.markFailed(jobId, safeError(failure));
                }
            }
            log.warn("[{}] Recovery job {} failed: {}", requestId(), jobId, safeError(failure));
        }
    }

    private void executeRecovery(RecoveryJob job) throws Exception {
        String strategy = Optional.ofNullable(job.getStrategy()).orElse("").toUpperCase(Locale.ROOT);
        switch (strategy) {
            case "PAYMENT_LINK" -> executePaymentLink(job);
            case "RETRY" -> executeRetry(job);
            case "RECONCILE_CAPTURED" -> reconcileCaptured(job);
            case "MANUAL" -> verifyManualHandoff(job);
            default -> throw new IllegalStateException("Unsupported recovery strategy");
        }
    }

    private void executePaymentLink(RecoveryJob job) {
        String key = idempotencyKey(job.getIncidentId());
        Optional<ProviderOrder> existing = providerOrders.findByIdempotencyKey(key);
        if (existing.filter(order -> PAID.contains(order.getStatus().toUpperCase(Locale.ROOT))).isPresent()) {
            recordOutcome(job, null, existing.get().getAmountPaise(), "provider-order:" + existing.get().getId());
            return;
        }

        if (!razorpayProperties.enabled()) {
            RecoveryAction action = requirePermittedAction(job);
            razorpay.createPaymentLink(job.getIncidentId(), action.getAmountMinor(),
                    "Sentinel recovery for interrupted payment", key);
            return;
        }

        RecoveryExecutionResponse result = executions.execute(job.getIncidentId());
        if (result.actionStatus() == RecoveryActionStatus.EXECUTION_UNCERTAIN
                || result.actionStatus() == RecoveryActionStatus.RETRY_PENDING
                || result.actionStatus() == RecoveryActionStatus.FAILED) {
            throw new IllegalStateException("Provider execution did not reach a durable state");
        }
        if (result.providerId() != null && existing.isEmpty()) {
            RecoveryAction action = requireAction(job);
            ProviderOrder mirror = new ProviderOrder(job.getIncidentId(), result.providerId(),
                    action.getAmountMinor(), Optional.ofNullable(action.getCurrency()).orElse("INR"),
                    Optional.ofNullable(result.providerStatus()).orElse("CREATED").toUpperCase(Locale.ROOT),
                    result.shortUrl(), key);
            try {
                providerOrders.saveAndFlush(mirror);
            } catch (DataIntegrityViolationException duplicate) {
                providerOrders.findByIdempotencyKey(key).orElseThrow(() -> duplicate);
            }
        }
    }

    private void executeRetry(RecoveryJob job) {
        RecoveryAction action = requirePermittedAction(job);
        long amount = action.getAmountMinor();
        if (amount <= 0) {
            throw new IllegalStateException("Recovery amount must be positive");
        }
        razorpay.createOrder(job.getIncidentId(), amount,
                Optional.ofNullable(action.getCurrency()).orElse("INR"),
                idempotencyKey(job.getIncidentId()));
    }

    private void verifyManualHandoff(RecoveryJob job) {
        RecoveryAction action = requireAction(job);
        if (action.getStatus() != RecoveryActionStatus.PENDING_APPROVAL
                && action.getStatus() != RecoveryActionStatus.APPROVED) {
            throw new IllegalStateException("Manual recovery has no persisted human-review action");
        }
    }

    private void reconcileCaptured(RecoveryJob job) throws Exception {
        WebhookEvent event = webhookEvents.findUnprocessed().stream()
                .filter(candidate -> job.getIncidentId().equals(candidate.getIncidentId()))
                .filter(candidate -> "payment.captured".equals(candidate.getEventType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Captured webhook event is unavailable"));
        JsonNode payment = json.readTree(event.getPayload())
                .path("payload").path("payment").path("entity");
        String paymentId = payment.path("id").asText();
        if (paymentId.isBlank()) {
            throw new IllegalArgumentException("Captured payment id is required");
        }
        String orderId = payment.path("order_id").asText();
        long amount = payment.path("amount").asLong(0);
        ProviderOrder order = providerOrders.findByRazorpayOrderId(orderId)
                .orElseGet(() -> providerOrders.findByIdempotencyKey(idempotencyKey(job.getIncidentId()))
                        .orElse(null));
        if (providerPayments.findByRazorpayPaymentId(paymentId).isEmpty()) {
            String rawPayment = payment.toString();
            ProviderPayment captured = new ProviderPayment(order == null ? null : order.getId(), paymentId,
                    orderId.isBlank() ? "unknown" : orderId, "CAPTURED", amount,
                    payment.path("method").asText(null), capturedAt(payment), rawPayment);
            try {
                providerPayments.saveAndFlush(captured);
            } catch (DataIntegrityViolationException duplicate) {
                providerPayments.findByRazorpayPaymentId(paymentId).orElseThrow(() -> duplicate);
            }
        }
        if (order != null && !PAID.contains(order.getStatus().toUpperCase(Locale.ROOT))) {
            order.updateProviderState("PAID", order.getProviderReference());
            providerOrders.saveAndFlush(order);
        }
        recordOutcome(job, event, amount, event.getEventId());
        webhookEvents.markProcessed(event.getId(), job.getIncidentId());
    }

    private void recordOutcome(RecoveryJob job, WebhookEvent event, long amount, String sourceEventId) {
        RecoveryAction action = requireAction(job);
        RevenueIncident incident = incidents.findById(job.getIncidentId())
                .orElseThrow(() -> new IllegalArgumentException("Revenue incident not found"));
        RecoveryOutcome outcome = outcomes.findByRecoveryActionId(action.getId())
                .orElseGet(() -> new RecoveryOutcome(action, incident, RecoveryOutcomeStatus.RECOVERED,
                        amount, Instant.now(), sourceEventId));
        if (outcome.getId() != null) {
            outcome.applyRecovered(Math.max(amount, outcome.getRecoveredAmountMinor()),
                    Instant.now(), sourceEventId);
        }
        outcomes.saveAndFlush(outcome);
        if (action.getExternalResourceId() != null && action.getStatus() != RecoveryActionStatus.RECOVERED) {
            action.recordRecovered("paid");
            actions.saveAndFlush(action);
        }
        if (incident.getStatus() != RevenueIncidentStatus.RECOVERED) {
            incident.transitionTo(stateMachine.transition(incident.getStatus(), RevenueIncidentStatus.RECOVERED));
            incidents.saveAndFlush(incident);
        }
    }

    private RecoveryAction requirePermittedAction(RecoveryJob job) {
        RecoveryAction action = requireAction(job);
        boolean permitted = action.getStatus() == RecoveryActionStatus.AUTO_APPROVED
                || action.getStatus() == RecoveryActionStatus.APPROVED
                || action.getStatus() == RecoveryActionStatus.RETRY_PENDING
                || action.getStatus() == RecoveryActionStatus.EXECUTION_UNCERTAIN;
        if (!permitted) {
            throw new IllegalStateException("Recovery action lacks persisted execution permission");
        }
        return action;
    }

    private RecoveryAction requireAction(RecoveryJob job) {
        if (job.getPolicyDecisionId() == null) {
            throw new IllegalStateException("Recovery job has no persisted policy action");
        }
        RecoveryAction action = actions.findById(job.getPolicyDecisionId())
                .orElseThrow(() -> new IllegalStateException("Persisted recovery action is unavailable"));
        if (!job.getIncidentId().equals(action.getIncidentId())) {
            throw new IllegalStateException("Recovery action does not belong to the incident");
        }
        return action;
    }

    private Instant capturedAt(JsonNode payment) {
        long epochSeconds = payment.path("captured_at").asLong(0);
        return epochSeconds > 0 ? Instant.ofEpochSecond(epochSeconds) : Instant.now();
    }

    private String idempotencyKey(UUID incidentId) {
        return "recovery-" + incidentId;
    }

    private String safeError(Exception failure) {
        String value = failure.getMessage();
        if (value == null || value.isBlank()) {
            value = failure.getClass().getSimpleName();
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private String requestId() {
        String requestId = RequestContext.getRequestId();
        return requestId == null ? "recovery-worker" : requestId;
    }
}
