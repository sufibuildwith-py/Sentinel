package com.sentinel.revenue.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.revenue.audit.AuditLogService;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import com.sentinel.revenue.service.RevenueIncidentStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebhookOutcomeProcessor {
    private static final String PAID = "payment_link.paid";
    private static final String PARTIAL = "payment_link.partially_paid";
    private static final String CANCELLED = "payment_link.cancelled";
    private final ProcessedWebhookEventRepository webhooks;
    private final RecoveryActionRepository actions;
    private final RevenueIncidentRepository incidents;
    private final RecoveryOutcomeRepository outcomes;
    private final RecoveryPlanRepository plans;
    private final HistoricalIncidentRepository history;
    private final ObjectMapper json;
    private final AuditLogService audit;
    private final WebhookSecurityAuditService securityAudit;
    private final RevenueIncidentStateMachine stateMachine = new RevenueIncidentStateMachine();

    public WebhookOutcomeProcessor(ProcessedWebhookEventRepository webhooks,
                                   RecoveryActionRepository actions, RevenueIncidentRepository incidents,
                                   RecoveryOutcomeRepository outcomes, RecoveryPlanRepository plans,
                                   HistoricalIncidentRepository history, ObjectMapper json,
                                   AuditLogService audit, WebhookSecurityAuditService securityAudit) {
        this.webhooks = webhooks; this.actions = actions; this.incidents = incidents;
        this.outcomes = outcomes; this.plans = plans; this.history = history;
        this.json = json; this.audit = audit; this.securityAudit = securityAudit;
    }

    @Transactional(readOnly = true)
    public boolean alreadyProcessed(String eventId) { return webhooks.existsByEventId(eventId); }

    @Transactional
    public WebhookResult process(String eventId, byte[] rawBody, String digest) {
        ProcessedWebhookEvent ledger = webhooks.saveAndFlush(
                ProcessedWebhookEvent.received(eventId, digest, Instant.now()));
        JsonNode root;
        try { root = json.readTree(rawBody); }
        catch (Exception malformed) {
            ledger.complete("MALFORMED", null, Map.of(), "REJECTED", "MALFORMED_JSON", Instant.now());
            webhooks.saveAndFlush(ledger);
            securityAudit.record(digest, true, true, "MALFORMED_JSON");
            return new WebhookResult(eventId, "REJECTED", false, "Malformed signed payload rejected");
        }
        String eventType = root.path("event").asText();
        JsonNode link = root.path("payload").path("payment_link").path("entity");
        String linkId = link.path("id").asText();
        long amount = link.path("amount").asLong(-1);
        long amountPaid = link.path("amount_paid").asLong(-1);
        String currency = link.path("currency").asText();
        String providerStatus = link.path("status").asText();
        Map<String, Object> minimized = minimized(eventType, linkId, amount, amountPaid, currency, providerStatus);

        if (!List.of(PAID, PARTIAL, CANCELLED).contains(eventType)) {
            ledger.complete(eventType.isBlank() ? "UNKNOWN" : eventType, linkId, minimized,
                    "IGNORED", "UNSUPPORTED_EVENT", Instant.now());
            webhooks.saveAndFlush(ledger);
            return new WebhookResult(eventId, "IGNORED", false, "Unsupported event ignored");
        }
        if (linkId.isBlank()) return reject(ledger, eventId, eventType, null, minimized,
                "MISSING_LINK_ID", digest);

        RecoveryAction action = actions.findForWebhookByExternalResourceId(linkId).orElse(null);
        if (action == null) {
            ledger.complete(eventType, linkId, minimized, "IGNORED", "UNKNOWN_LINK", Instant.now());
            webhooks.saveAndFlush(ledger);
            securityAudit.record(digest, true, true, "IGNORED_UNKNOWN_LINK");
            return new WebhookResult(eventId, "IGNORED", false, "Non-Sentinel Payment Link ignored");
        }
        RevenueIncident incident = incidents.findById(action.getIncidentId()).orElseThrow();
        audit.appendExternal(incident, "RAZORPAY_WEBHOOK", "WEBHOOK_ACCEPTED",
                List.of("eventId=" + eventId, "eventType=" + eventType, "payloadDigest=" + digest),
                "Signed event accepted", linkId, "TEST");

        String invalid = validate(action, eventType, amount, amountPaid, currency, providerStatus, incident);
        if (invalid != null) {
            ledger.complete(eventType, linkId, minimized, "REJECTED", invalid, Instant.now());
            webhooks.saveAndFlush(ledger);
            audit.appendExternal(incident, "RAZORPAY_WEBHOOK", "WEBHOOK_REJECTED",
                    List.of("eventId=" + eventId, "safeReason=" + invalid),
                    "No revenue state changed", linkId, "TEST");
            return new WebhookResult(eventId, "REJECTED", false, "Signed event failed consistency checks");
        }

        RecoveryOutcome outcome = outcomes.findByRecoveryActionId(action.getId()).orElse(null);
        boolean changed = switch (eventType) {
            case PARTIAL -> applyPartial(action, incident, outcome, amountPaid, eventId, providerStatus);
            case PAID -> applyPaid(action, incident, outcome, amountPaid, eventId, providerStatus);
            case CANCELLED -> applyCancelled(action, incident, outcome, eventId, providerStatus);
            default -> false;
        };
        String disposition = changed ? "APPLIED" : "IGNORED_STALE";
        ledger.complete(eventType, linkId, minimized, disposition, null, Instant.now());
        webhooks.saveAndFlush(ledger);
        audit.appendExternal(incident, "RAZORPAY_WEBHOOK",
                changed ? "WEBHOOK_APPLIED" : "WEBHOOK_IGNORED_STALE",
                List.of("eventId=" + eventId, "eventType=" + eventType,
                        "cumulativeAmountPaid=" + amountPaid),
                changed ? "Current outcome projection updated" : "Monotonic state retained",
                linkId, "TEST");
        return new WebhookResult(eventId, disposition, false,
                changed ? "Outcome applied exactly once" : "Older state ignored");
    }

    @Transactional
    public WebhookResult duplicate(String eventId) {
        ProcessedWebhookEvent existing = webhooks.findByEventId(eventId).orElse(null);
        if (existing != null && existing.getProviderLinkId() != null) {
            actions.findByExternalResourceId(existing.getProviderLinkId()).ifPresent(action ->
                    incidents.findById(action.getIncidentId()).ifPresent(incident ->
                            audit.appendExternal(incident, "RAZORPAY_WEBHOOK", "WEBHOOK_DUPLICATE",
                                    List.of("eventId=" + eventId), "No state or metric change",
                                    existing.getProviderLinkId(), "TEST")));
        }
        return new WebhookResult(eventId, "DUPLICATE", true, "Event was already processed");
    }

    private boolean applyPartial(RecoveryAction action, RevenueIncident incident, RecoveryOutcome outcome,
                                 long amountPaid, String eventId, String providerStatus) {
        if (incident.getStatus() != RevenueIncidentStatus.MONITORING) return false;
        boolean changed;
        if (outcome == null) {
            outcome = new RecoveryOutcome(action, incident, RecoveryOutcomeStatus.PARTIALLY_RECOVERED,
                    amountPaid, Instant.now(), eventId); changed = true;
        } else changed = outcome.applyPartial(amountPaid, Instant.now(), eventId);
        if (!changed) return false;
        action.recordPartial(providerStatus); actions.saveAndFlush(action); outcomes.saveAndFlush(outcome);
        audit.appendExternal(incident, "RAZORPAY_WEBHOOK", "RECOVERY_PARTIAL",
                List.of("cumulativeRecoveredMinor=" + amountPaid),
                "Incident remains MONITORING", action.getExternalResourceId(), "METRIC_UPDATED");
        return true;
    }

    private boolean applyPaid(RecoveryAction action, RevenueIncident incident, RecoveryOutcome outcome,
                              long amountPaid, String eventId, String providerStatus) {
        boolean changed;
        if (outcome == null) {
            outcome = new RecoveryOutcome(action, incident, RecoveryOutcomeStatus.RECOVERED,
                    amountPaid, Instant.now(), eventId); changed = true;
        } else changed = outcome.applyRecovered(amountPaid, Instant.now(), eventId);
        if (!changed) return false;
        action.recordRecovered(providerStatus); actions.saveAndFlush(action); outcomes.saveAndFlush(outcome);
        if (incident.getStatus() != RevenueIncidentStatus.RECOVERED)
            transition(incident, RevenueIncidentStatus.RECOVERED, "Verified Payment Link paid");
        if (!history.existsByOriginalIncidentIncidentId(incident.getIncidentId())) {
            RecoveryPlan plan = plans.findById(action.getRecoveryPlanId()).orElseThrow();
            history.saveAndFlush(new HistoricalIncident(incident,
                    incident.getRootCause() == null ? incident.getType() : incident.getRootCause(),
                    Map.of("verifiedEventId", eventId, "providerLinkId", action.getExternalResourceId(),
                            "recoveredAmountMinor", amountPaid), plan.getStrategy(),
                    RecoveryOutcomeStatus.RECOVERED, amountPaid, Instant.now()));
            audit.appendExternal(incident, "SENTINEL_MEMORY", "HISTORICAL_MEMORY_RECORDED",
                    List.of("strategy=" + plan.getStrategy(), "recoveredAmountMinor=" + amountPaid),
                    "Terminal outcome retained for future planning", action.getExternalResourceId(), "TEST");
        }
        audit.appendExternal(incident, "RAZORPAY_WEBHOOK", "RECOVERY_METRIC_UPDATED",
                List.of("recoveredMinor=" + amountPaid, "attemptedMinor=" + action.getAmountMinor()),
                "Latest verified cumulative amount applied", action.getExternalResourceId(), "TEST");
        return true;
    }

    private boolean applyCancelled(RecoveryAction action, RevenueIncident incident, RecoveryOutcome outcome,
                                   String eventId, String providerStatus) {
        if (outcome != null && outcome.isTerminalPaid()) return false;
        boolean changed;
        if (outcome == null) {
            outcome = new RecoveryOutcome(action, incident, RecoveryOutcomeStatus.STOPPED,
                    0, Instant.now(), eventId); changed = true;
        } else changed = outcome.applyCancelled(Instant.now(), eventId);
        if (!changed) return false;
        action.recordCancelled(providerStatus); actions.saveAndFlush(action); outcomes.saveAndFlush(outcome);
        if (incident.getStatus() == RevenueIncidentStatus.MONITORING)
            transition(incident, RevenueIncidentStatus.STOPPED, "Payment Link cancelled without recovery");
        audit.appendExternal(incident, "RAZORPAY_WEBHOOK", "RECOVERY_CANCELLED",
                List.of("recoveredMinor=0"), "No recovered revenue recorded",
                action.getExternalResourceId(), "TEST");
        return true;
    }

    private String validate(RecoveryAction action, String eventType, long amount, long amountPaid,
                            String currency, String status, RevenueIncident incident) {
        if (amount != action.getAmountMinor()) return "AMOUNT_MISMATCH";
        if (action.getCurrency() == null || !action.getCurrency().equalsIgnoreCase(currency)) return "CURRENCY_MISMATCH";
        if (amountPaid < 0 || amountPaid > amount) return "INVALID_AMOUNT_PAID";
        if (PAID.equals(eventType) && !"paid".equalsIgnoreCase(status)) return "STATUS_MISMATCH";
        if (PARTIAL.equals(eventType) && !"partially_paid".equalsIgnoreCase(status)) return "STATUS_MISMATCH";
        if (CANCELLED.equals(eventType) && !"cancelled".equalsIgnoreCase(status)) return "STATUS_MISMATCH";
        if (PARTIAL.equals(eventType) && amountPaid == 0) return "INVALID_PARTIAL_AMOUNT";
        if (PAID.equals(eventType) && amountPaid == 0) return "INVALID_PAID_AMOUNT";
        if (!List.of(RevenueIncidentStatus.MONITORING, RevenueIncidentStatus.STOPPED,
                RevenueIncidentStatus.RECOVERED).contains(incident.getStatus())) return "INVALID_INCIDENT_STATE";
        return null;
    }

    private WebhookResult reject(ProcessedWebhookEvent ledger, String eventId, String type, String linkId,
                                 Map<String, Object> minimized, String reason, String digest) {
        ledger.complete(type, linkId, minimized, "REJECTED", reason, Instant.now());
        webhooks.saveAndFlush(ledger); securityAudit.record(digest, true, true, reason);
        return new WebhookResult(eventId, "REJECTED", false, "Signed event rejected safely");
    }
    private Map<String, Object> minimized(String event, String linkId, long amount, long amountPaid,
                                          String currency, String status) {
        Map<String, Object> safe = new LinkedHashMap<>(); safe.put("event", event); safe.put("linkId", linkId);
        safe.put("amountMinor", amount); safe.put("amountPaidMinor", amountPaid);
        safe.put("currency", currency); safe.put("status", status); return safe;
    }
    private void transition(RevenueIncident incident, RevenueIncidentStatus target, String reason) {
        RevenueIncidentStatus previous = incident.getStatus();
        incident.transitionTo(stateMachine.transition(previous, target)); incidents.saveAndFlush(incident);
        audit.append(incident, "RAZORPAY_WEBHOOK", null, "STATE_TRANSITION", List.of(reason), null,
                reason, List.of(), null, previous, target, reason);
    }
}
