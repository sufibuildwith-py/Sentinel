package com.sentinel.revenue.execution;

import com.sentinel.revenue.audit.AuditLogService;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.PaymentEventRepository;
import com.sentinel.revenue.repository.RecoveryActionRepository;
import com.sentinel.revenue.repository.RecoveryPlanRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import com.sentinel.revenue.service.RevenueIncidentStateMachine;
import com.sentinel.revenue.governor.GovernorEvaluation;
import com.sentinel.revenue.governor.RecoverySafetyGovernor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class RecoveryExecutionService {
    private static final Set<String> PAID = Set.of("AUTHORIZED", "CAPTURED", "PAID", "REFUNDED");
    private final RecoveryActionRepository actions;
    private final RecoveryPlanRepository plans;
    private final RevenueIncidentRepository incidents;
    private final PaymentEventRepository payments;
    private final RazorpayGateway gateway;
    private final RazorpayProperties properties;
    private final AuditLogService audit;
    private final RecoverySafetyGovernor governor;
    private final RecoveryExecutionEligibilityEvaluator eligibility;
    private final RevenueIncidentStateMachine stateMachine = new RevenueIncidentStateMachine();

    @Autowired
    public RecoveryExecutionService(RecoveryActionRepository actions, RecoveryPlanRepository plans,
                                    RevenueIncidentRepository incidents, PaymentEventRepository payments,
                                    RazorpayGateway gateway, RazorpayProperties properties,
                                    AuditLogService audit, RecoverySafetyGovernor governor,
                                    RecoveryExecutionEligibilityEvaluator eligibility) {
        this.actions = actions; this.plans = plans; this.incidents = incidents; this.payments = payments;
        this.gateway = gateway; this.properties = properties; this.audit = audit;
        this.governor = governor;
        this.eligibility = eligibility;
    }

    public RecoveryExecutionService(RecoveryActionRepository actions, RecoveryPlanRepository plans,
                                    RevenueIncidentRepository incidents, PaymentEventRepository payments,
                                    RazorpayGateway gateway, RazorpayProperties properties,
                                    AuditLogService audit) {
        this(actions, plans, incidents, payments, gateway, properties, audit, null,
                new RecoveryExecutionEligibilityEvaluator(properties));
    }

    public RecoveryExecutionService(RecoveryActionRepository actions, RecoveryPlanRepository plans,
                                    RevenueIncidentRepository incidents, PaymentEventRepository payments,
                                    RazorpayGateway gateway, RazorpayProperties properties,
                                    AuditLogService audit, RecoverySafetyGovernor governor) {
        this(actions, plans, incidents, payments, gateway, properties, audit, governor,
                new RecoveryExecutionEligibilityEvaluator(properties));
    }

    @Transactional
    public RecoveryExecutionResponse execute(UUID incidentId) {
        RecoveryAction action = actions.findForExecutionByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Recovery action not found for incident: " + incidentId));
        RevenueIncident incident = incidents.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Revenue incident not found: " + incidentId));
        RecoveryPlan plan = plans.findById(action.getRecoveryPlanId())
                .orElseThrow(() -> new IllegalStateException("Recovery plan not found"));

        if (action.getStatus() == RecoveryActionStatus.EXECUTED && action.getExternalResourceId() != null)
            return response(incidentId, action, true, "Existing Test Mode link returned");
        Instant now = Instant.now();
        if (action.getExecutionAttempts() >= properties.maximumAttempts()) {
            action.stop("MAX_EXECUTION_ATTEMPTS"); actions.saveAndFlush(action);
            stopIncident(incident, RevenueIncidentStatus.STOPPED, "Maximum execution attempts reached");
            audit.appendExternal(incident, "EXECUTOR", "EXECUTION_STOPPED",
                    List.of("attempts=" + action.getExecutionAttempts(),
                            "maximum=" + properties.maximumAttempts()),
                    "No provider call", null, "MAX_EXECUTION_ATTEMPTS");
            return response(incidentId, action, false, "Maximum execution attempts reached");
        }
        RecoveryExecutionEligibility currentEligibility = eligibility.evaluate(action, plan, null);
        if (!currentEligibility.eligible()) throw new RecoveryExecutionUnavailableException(currentEligibility);

        if (now.isAfter(action.getCreatedAt().plus(properties.actionExpiry()))) {
            action.stop("ACTION_EXPIRED");
            actions.saveAndFlush(action);
            stopIncident(incident, RevenueIncidentStatus.STOPPED, "Execution window expired");
            audit.appendExternal(incident, "EXECUTOR", "EXECUTION_STOPPED", List.of("actionExpired=true"),
                    "No provider call", null, "ACTION_EXPIRED");
            return response(incidentId, action, false, "Action expired before execution");
        }
        PaymentEvent target = selectTarget(incident);
        if (PAID.contains(target.getStatus().toUpperCase(Locale.ROOT))) {
            action.stop("PAYMENT_ALREADY_PAID"); actions.saveAndFlush(action);
            stopIncident(incident, RevenueIncidentStatus.STOPPED, "Local payment is already settled");
            audit.appendExternal(incident, "EXECUTOR", "EXECUTION_STOPPED", List.of("alreadyPaid=true"),
                    "Local state prevented provider call", null, "TEST");
            return response(incidentId, action, false, "Original payment is already settled");
        }
        if (governor != null) {
            GovernorEvaluation evaluation = governor.evaluate(action, plan.getStrategy(), target.getAmountMinor(), now);
            audit.append(incident, "GOVERNOR", null, "BLAST_RADIUS_EVALUATED",
                    evaluation.violations().isEmpty() ? List.of("allowedValueMinor=" + evaluation.allowedValueMinor())
                            : evaluation.violations(), null,
                    evaluation.allowed() ? "Execution envelope granted" : "Execution envelope denied",
                    List.of(), evaluation.allowed() ? "ALLOW" : "DENY", null, null,
                    evaluation.allowed() ? "Execution may continue" : "No provider call");
            if (!evaluation.allowed()) {
                action.stop("GOVERNOR_DENIED");
                actions.saveAndFlush(action);
                stopIncident(incident, RevenueIncidentStatus.STOPPED,
                        "Recovery safety governor denied execution");
                return response(incidentId, action, false,
                        "Recovery safety governor denied execution; no provider call was made");
            }
        }
        String reference = action.getProviderReferenceId() == null
                ? reference(action.getId()) : action.getProviderReferenceId();
        action.claim(target.getPaymentId(), target.getCustomerId(), target.getCurrency(), target.getAmountMinor(),
                reference, now, now.plus(properties.linkExpiry()));
        actions.saveAndFlush(action);
        if (incident.getStatus() == RevenueIncidentStatus.APPROVED)
            transition(incident, RevenueIncidentStatus.EXECUTING, "Test Mode execution claimed");
        audit.appendExternal(incident, "EXECUTOR", "EXECUTION_CLAIMED",
                List.of("target=" + masked(target.getPaymentId()), "amountMinor=" + target.getAmountMinor(),
                        "currency=" + target.getCurrency(), "outsideScopeCount="
                                + Math.max(0, incident.getAffectedPayments().size() - 1)),
                "One failed payment selected deterministically", null, "TEST");

        try {
            // Synthetic/demo payment IDs intentionally use the same readable
            // prefix as Razorpay IDs, but they do not exist at the provider.
            // Only provider-backed events may be fetched remotely.
            if (isProviderBacked(target)) {
                ProviderPayment providerPayment = gateway.fetchPayment(target.getPaymentId());
                audit.appendExternal(incident, "RAZORPAY_TEST", "ORIGINAL_PAYMENT_VERIFIED",
                        List.of("target=" + masked(target.getPaymentId()), "status=" + providerPayment.status()),
                        "Provider-backed payment checked", null, "TEST");
                if (providerPayment.isAlreadyPaid()) return stopAlreadyPaid(incident, action);
            } else {
                audit.appendExternal(incident, "EXECUTOR", "ORIGINAL_PAYMENT_VERIFIED",
                        List.of("providerBacked=false", "localStatus=" + target.getStatus()),
                        "Local failed status checked", null, "TEST");
            }

            Optional<PaymentLinkResource> reconciled = gateway.findPaymentLinkByReference(reference);
            audit.appendExternal(incident, "RAZORPAY_TEST", "RECONCILIATION",
                    List.of("reference=" + reference, "found=" + reconciled.isPresent()),
                    "Pre-create reference lookup", reconciled.map(PaymentLinkResource::id).orElse(null), "TEST");
            if (reconciled.isPresent()) return complete(incident, action, reconciled.get(), true);

            PaymentLinkCommand command = command(action, target, plan);
            audit.appendExternal(incident, "EXECUTOR", "PROVIDER_REQUEST",
                    List.of("amountMinor=" + command.amountMinor(), "currency=" + command.currency(),
                            "acceptPartial=false", "upiEnabled=false", "notifications=" + command.notificationsEnabled()),
                    "Create Razorpay Standard Payment Link", null, "TEST");
            try {
                return complete(incident, action, gateway.createPaymentLink(command), false);
            } catch (RazorpayFailure createFailure) {
                if (createFailure.kind() == RazorpayFailure.Kind.AMBIGUOUS
                        || createFailure.kind() == RazorpayFailure.Kind.TEMPORARY) {
                    return reconcileAfterAmbiguous(incident, action, reference, createFailure);
                }
                return fail(incident, action, createFailure);
            }
        } catch (RazorpayFailure failure) {
            if (failure.kind() == RazorpayFailure.Kind.NON_RETRYABLE
                    || failure.kind() == RazorpayFailure.Kind.MALFORMED) {
                return fail(incident, action, failure);
            }
            return pending(incident, action, failure, isUncertain(failure));
        }
    }

    @Transactional
    public RecoveryExecutionResponse cancel(UUID actionId) {
        RecoveryAction action = actions.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Recovery action not found: " + actionId));
        RevenueIncident incident = incidents.findById(action.getIncidentId()).orElseThrow();
        if (action.getExternalResourceId() == null) throw new IllegalStateException("Action has no provider resource");
        PaymentLinkResource cancelled = gateway.cancelPaymentLink(action.getExternalResourceId());
        action.stop("CANCELLED"); actions.saveAndFlush(action);
        audit.appendExternal(incident, "RAZORPAY_TEST", "EXECUTION_CANCELLED", List.of("status=" + cancelled.status()),
                "Payment Link cancelled", cancelled.id(), "TEST");
        return response(incident.getIncidentId(), action, true, "Test Mode link cancelled");
    }

    @Transactional(readOnly = true)
    public void notify(UUID actionId, String medium) {
        RecoveryAction action = actions.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Recovery action not found: " + actionId));
        if (action.getExternalResourceId() == null) throw new IllegalStateException("Action has no provider resource");
        gateway.resendNotification(action.getExternalResourceId(), medium);
    }

    private RecoveryExecutionResponse reconcileAfterAmbiguous(RevenueIncident incident, RecoveryAction action,
                                                               String reference, RazorpayFailure original) {
        try {
            Optional<PaymentLinkResource> recovered = gateway.findPaymentLinkByReference(reference);
            audit.appendExternal(incident, "RAZORPAY_TEST", "RECONCILIATION",
                    List.of("reference=" + reference, "found=" + recovered.isPresent(),
                            "after=" + original.safeCode()), "Ambiguous create reconciled",
                    recovered.map(PaymentLinkResource::id).orElse(null), "TEST");
            return recovered.map(link -> complete(incident, action, link, true))
                    .orElseGet(() -> pending(incident, action, original, true));
        } catch (RazorpayFailure reconciliationFailure) {
            return pending(incident, action, reconciliationFailure, true);
        }
    }

    private RecoveryExecutionResponse complete(RevenueIncident incident, RecoveryAction action,
                                               PaymentLinkResource link, boolean existing) {
        action.complete(link.id(), link.shortUrl(), link.status(), Instant.now(),
                ExecutionMode.RAZORPAY_TEST_MODE);
        actions.saveAndFlush(action);
        if (incident.getStatus() == RevenueIncidentStatus.EXECUTING)
            transition(incident, RevenueIncidentStatus.MONITORING, "Payment Link created; awaiting webhook phase");
        audit.appendExternal(incident, "RAZORPAY_TEST", "EXECUTION_SUCCESS",
                List.of("reference=" + link.referenceId(), "providerStatus=" + link.status()),
                existing ? "Recovered existing link" : "Created one link", link.id(), "TEST");
        return response(incident.getIncidentId(), action, existing, existing ? "Existing Test Mode link recovered" : "Test Mode link created");
    }

    private RecoveryExecutionResponse pending(RevenueIncident incident, RecoveryAction action,
                                              RazorpayFailure failure, boolean uncertain) {
        action.retryPending(failure.safeCode(), uncertain); actions.saveAndFlush(action);
        audit.appendExternal(incident, "RAZORPAY_TEST", uncertain ? "EXECUTION_UNCERTAIN" : "RETRY_PENDING",
                List.of("safeError=" + failure.safeReason()), "No blind create retry",
                null, "TEST");
        return response(incident.getIncidentId(), action, false, "Provider unavailable; safe reconciliation required");
    }

    private boolean isUncertain(RazorpayFailure failure) {
        return failure.kind() == RazorpayFailure.Kind.AMBIGUOUS
                || failure.kind() == RazorpayFailure.Kind.CIRCUIT_OPEN;
    }

    private RecoveryExecutionResponse fail(RevenueIncident incident, RecoveryAction action, RazorpayFailure failure) {
        action.executionFailed(failure.safeCode()); actions.saveAndFlush(action);
        if (incident.getStatus() == RevenueIncidentStatus.EXECUTING)
            transition(incident, RevenueIncidentStatus.FAILED, "Non-retryable provider rejection");
        audit.appendExternal(incident, "RAZORPAY_TEST", "EXECUTION_FAILED", List.of("safeError=" + failure.safeReason()),
                "Provider request rejected", null, "TEST");
        return response(incident.getIncidentId(), action, false, "Test Mode execution rejected");
    }

    private RecoveryExecutionResponse stopAlreadyPaid(RevenueIncident incident, RecoveryAction action) {
        action.stop("PAYMENT_ALREADY_PAID"); actions.saveAndFlush(action);
        stopIncident(incident, RevenueIncidentStatus.STOPPED, "Original payment already settled");
        audit.appendExternal(incident, "RAZORPAY_TEST", "EXECUTION_STOPPED", List.of("alreadyPaid=true"),
                "Duplicate-charge risk prevented link creation", null, "TEST");
        return response(incident.getIncidentId(), action, false, "Original payment is already settled");
    }

    private PaymentEvent selectTarget(RevenueIncident incident) {
        return payments.findAllByPaymentIdIn(incident.getAffectedPayments()).stream()
                .sorted(Comparator.comparing(PaymentEvent::getTimestamp).thenComparing(PaymentEvent::getPaymentId))
                .findFirst().orElseThrow(() -> new IllegalStateException("No payment exists for the action"));
    }
    private boolean isProviderBacked(PaymentEvent target) {
        return target.getPaymentId().startsWith("pay_")
                && !Boolean.TRUE.equals(target.getMetadata().get("synthetic"));
    }
    private PaymentLinkCommand command(RecoveryAction action, PaymentEvent target, RecoveryPlan plan) {
        return new PaymentLinkCommand(target.getAmountMinor(), target.getCurrency(), action.getProviderReferenceId(),
                "Sentinel recovery for interrupted payment", action.getExpiresAt(), action.getId(),
                masked(target.getCustomerId()), true, properties.notificationsEnabled());
    }
    private String reference(UUID actionId) { return "sntl_" + actionId.toString().replace("-", ""); }
    private String masked(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return "ref_" + HexFormat.of().formatHex(digest, 0, 4);
        } catch (Exception impossible) { throw new IllegalStateException("Masking unavailable"); }
    }
    private RecoveryExecutionResponse response(UUID incidentId, RecoveryAction action, boolean existing, String message) {
        return new RecoveryExecutionResponse(incidentId, action.getId(), action.getStatus(),
                action.getExternalResourceId(), action.getProviderReferenceId(), action.getExternalResourceUrl(),
                action.getExternalResourceStatus(), "TEST", existing, message);
    }
    private void stopIncident(RevenueIncident incident, RevenueIncidentStatus target, String reason) {
        if (incident.getStatus() == target) return;
        transition(incident, target, reason);
    }
    private void transition(RevenueIncident incident, RevenueIncidentStatus target, String reason) {
        RevenueIncidentStatus previous = incident.getStatus();
        incident.transitionTo(stateMachine.transition(previous, target)); incidents.saveAndFlush(incident);
        audit.append(incident, "EXECUTOR", null, "STATE_TRANSITION", List.of(reason), null,
                reason, List.of(), null, previous, target, reason);
    }
}
