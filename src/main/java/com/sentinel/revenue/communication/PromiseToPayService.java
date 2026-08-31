package com.sentinel.revenue.communication;

import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class PromiseToPayService {
    private static final Set<PromiseStatus> OPEN = EnumSet.of(PromiseStatus.PENDING,
            PromiseStatus.REMINDER_DUE, PromiseStatus.PARTIALLY_KEPT);
    private final PromiseToPayRepository promises;
    private final RevenueIncidentRepository incidents;
    private final RecoveryActionRepository actions;
    private final CustomerContactPreferenceRepository preferences;
    private final CommunicationProperties properties;
    public PromiseToPayService(PromiseToPayRepository promises, RevenueIncidentRepository incidents,
                               RecoveryActionRepository actions, CustomerContactPreferenceRepository preferences,
                               CommunicationProperties properties) {
        this.promises = promises; this.incidents = incidents; this.actions = actions;
        this.preferences = preferences; this.properties = properties;
    }
    @Transactional
    public PromiseToPay create(UUID incidentId, UUID actionId, String customerRef,
                               long promisedAmountMinor, long balanceMinor, Instant dueAt,
                               boolean ambiguous, Instant now) {
        Instant at = now == null ? Instant.now() : now;
        if (ambiguous) throw new IllegalArgumentException("AMBIGUOUS_PROMISE");
        if (promisedAmountMinor <= 0 || promisedAmountMinor > balanceMinor)
            throw new IllegalArgumentException("PROMISED_AMOUNT_OUTSIDE_BALANCE");
        if (dueAt == null || !dueAt.isAfter(at)
                || dueAt.isAfter(at.plus(Duration.ofDays(properties.maximumPromiseDays()))))
            throw new IllegalArgumentException("INVALID_PROMISE_DUE_DATE");
        RevenueIncident incident = incidents.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Revenue incident not found: " + incidentId));
        if (incident.getStatus() == RevenueIncidentStatus.RECOVERED
                || incident.getStatus() == RevenueIncidentStatus.STOPPED
                || incident.getStatus() == RevenueIncidentStatus.FAILED)
            throw new IllegalStateException("INCIDENT_NOT_PROMISE_ELIGIBLE");
        RecoveryAction action = actions.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Recovery action not found: " + actionId));
        boolean permitted = action.getPolicyDecision() == PolicyDecision.AUTO
                || (action.getPolicyDecision() == PolicyDecision.HUMAN && action.getApprovedAt() != null);
        if (!permitted) throw new IllegalStateException("POLICY_PERMISSION_REQUIRED");
        CustomerContactPreference preference = preferences.findById(customerRef)
                .orElseThrow(() -> new IllegalStateException("CONTACT_PREFERENCE_REQUIRED"));
        if (preference.isOptedOut() || preference.isDoNotContact())
            throw new IllegalStateException("CUSTOMER_CONTACT_PROHIBITED");
        return promises.saveAndFlush(new PromiseToPay(incidentId, actionId, customerRef,
                promisedAmountMinor, balanceMinor, dueAt, at));
    }
    @Transactional
    public List<PromiseToPay> applyProviderConfirmedPayment(UUID actionId, long amountMinor,
                                                            String sourceEventId, Instant occurredAt) {
        List<PromiseToPay> open = promises.findAllByRecoveryActionIdAndStatusIn(actionId, OPEN);
        open.forEach(promise -> { if (promise.applyConfirmedPayment(amountMinor, sourceEventId, occurredAt)) promises.save(promise); });
        return List.copyOf(open);
    }
}
