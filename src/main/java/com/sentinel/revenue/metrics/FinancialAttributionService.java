package com.sentinel.revenue.metrics;

import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class FinancialAttributionService {
    public static final String LABEL = "Financial Attribution — Provider-confirmed Test Mode truth";
    private final RevenueIncidentRepository incidents;
    private final PaymentEventRepository payments;
    private final IncidentFindingRepository findings;
    private final RecoveryActionRepository actions;
    private final RecoveryOutcomeRepository outcomes;
    public FinancialAttributionService(RevenueIncidentRepository incidents, PaymentEventRepository payments,
                                       IncidentFindingRepository findings, RecoveryActionRepository actions,
                                       RecoveryOutcomeRepository outcomes) {
        this.incidents = incidents; this.payments = payments; this.findings = findings;
        this.actions = actions; this.outcomes = outcomes;
    }

    @Transactional(readOnly = true)
    public FinancialAttribution attribution() {
        List<RevenueIncident> allIncidents = incidents.findAll();
        List<RecoveryAction> allActions = actions.findAll();
        Map<UUID, RecoveryAction> actionById = new HashMap<>();
        allActions.forEach(action -> actionById.put(action.getId(), action));
        long failed = allIncidents.stream().mapToLong(RevenueIncident::getAmountAtRiskMinor).sum();
        long ineligible = allActions.stream().filter(this::ineligible)
                .mapToLong(RecoveryAction::getAmountMinor).sum();
        long addressable = Math.max(0, failed - ineligible);
        long naturalRecovery = 0;
        long incrementalOpportunity = Math.max(0, addressable - naturalRecovery);
        long executed = allActions.stream().filter(action -> action.getExternalResourceId() != null)
                .mapToLong(RecoveryAction::getAmountMinor).sum();
        long confirmed = deduplicatedConfirmedRecovery(actionById, outcomes.findAll());
        long unreconciled = Math.max(0, executed - confirmed);
        long attributed = Math.max(0, confirmed - naturalRecovery);
        long cost = 0;
        return new FinancialAttribution(LABEL, failed, ineligible, addressable, naturalRecovery,
                "NOT_ESTIMATED_NO_CAUSAL_BASELINE", incrementalOpportunity, executed, confirmed,
                unreconciled, attributed, cost, "NOT_CONFIGURED", attributed - cost,
                timings(allIncidents, allActions));
    }

    private long deduplicatedConfirmedRecovery(Map<UUID, RecoveryAction> actionById,
                                               List<RecoveryOutcome> allOutcomes) {
        Set<String> sourceEvents = new HashSet<>();
        Map<String, Long> recoveredByPayment = new HashMap<>();
        for (RecoveryOutcome outcome : allOutcomes) {
            if (!outcome.isProviderConfirmed()) continue;
            if (outcome.getSourceEventId() != null && !sourceEvents.add(outcome.getSourceEventId())) continue;
            RecoveryAction action = actionById.get(outcome.getRecoveryActionId());
            if (action == null) continue;
            String paymentKey = action.getTargetPaymentId() == null
                    ? "action:" + action.getId() : "payment:" + action.getTargetPaymentId();
            recoveredByPayment.merge(paymentKey, outcome.getRecoveredAmountMinor(), Math::max);
        }
        return recoveredByPayment.values().stream().mapToLong(Long::longValue).sum();
    }

    private FinancialAttribution.OperationalTimings timings(List<RevenueIncident> allIncidents,
                                                             List<RecoveryAction> allActions) {
        Map<UUID, RevenueIncident> incidentById = new HashMap<>();
        allIncidents.forEach(incident -> incidentById.put(incident.getIncidentId(), incident));
        List<Long> ttd = new ArrayList<>();
        List<Long> tgd = new ArrayList<>();
        List<Long> tte = new ArrayList<>();
        List<Long> ttr = new ArrayList<>();
        for (RevenueIncident incident : allIncidents) {
            payments.findAllByPaymentIdIn(incident.getAffectedPayments()).stream()
                    .map(PaymentEvent::getTimestamp).min(Instant::compareTo)
                    .ifPresent(start -> ttd.add(nonNegativeMillis(start, incident.getDetectedAt())));
            findings.findAllByIncidentIncidentId(incident.getIncidentId()).stream()
                    .filter(finding -> finding.getSource() == FindingSource.ROOT_CAUSE_AGENT)
                    .map(IncidentFinding::getCreatedAt).min(Instant::compareTo)
                    .ifPresent(diagnosed -> tgd.add(nonNegativeMillis(incident.getDetectedAt(), diagnosed)));
        }
        for (RecoveryAction action : allActions) {
            RevenueIncident incident = incidentById.get(action.getIncidentId());
            if (incident == null) continue;
            if (action.getExecutedAt() != null)
                tte.add(nonNegativeMillis(incident.getDetectedAt(), action.getExecutedAt()));
            outcomes.findByRecoveryActionId(action.getId()).filter(RecoveryOutcome::isProviderConfirmed)
                    .ifPresent(outcome -> ttr.add(nonNegativeMillis(incident.getDetectedAt(), outcome.getOccurredAt())));
        }
        return new FinancialAttribution.OperationalTimings(
                metric(ttd, "first failed payment event → incident detected"),
                metric(tgd, "incident detected → grounded diagnosis"),
                metric(tte, "incident detected → provider execution accepted"),
                metric(ttr, "incident detected → provider-confirmed reconciliation"));
    }

    private FinancialAttribution.TimingMetric metric(List<Long> samples, String definition) {
        Long average = samples.isEmpty() ? null
                : Math.round(samples.stream().mapToLong(Long::longValue).average().orElse(0));
        return new FinancialAttribution.TimingMetric(average, samples.size(), definition);
    }
    private long nonNegativeMillis(Instant start, Instant end) {
        return Math.max(0, Duration.between(start, end).toMillis());
    }
    private boolean ineligible(RecoveryAction action) { return switch (action.getStatus()) {
        case REJECTED, STOPPED, FAILED, CANCELLED -> true;
        default -> action.getPolicyDecision() == PolicyDecision.DENY;
    }; }
}
