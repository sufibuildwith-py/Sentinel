package com.sentinel.revenue.opportunity;

import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.Instant;
import java.util.*;

@Service
public class RecoveryOpportunityEngine {
    private final ProviderCapabilityRegistry capabilities;
    private final HistoricalIncidentRepository history;
    private final RecoveryOpportunityLogRepository logs;
    private final OpportunityProperties properties;
    public RecoveryOpportunityEngine(ProviderCapabilityRegistry capabilities,
                                     HistoricalIncidentRepository history,
                                     RecoveryOpportunityLogRepository logs,
                                     OpportunityProperties properties) {
        this.capabilities = capabilities; this.history = history; this.logs = logs; this.properties = properties;
    }
    @Transactional
    public RecoveryOpportunityDecision evaluate(RevenueIncident incident, RecoveryStrategy fallbackStrategy) {
        List<HistoricalIncident> historical = history.findAll();
        List<ActionOpportunity> candidates = capabilities.supportedActions().stream()
                .sorted(Comparator.comparing(Enum::name)).map(action -> candidate(action, incident, historical)).toList();
        OpportunityAction choice = candidates.stream().filter(candidate -> candidate.actionRecoveryProbability() != null)
                .max(Comparator.comparing(ActionOpportunity::actionRecoveryProbability))
                .map(ActionOpportunity::action).orElse(OpportunityAction.NO_ACTION);
        Instant now = Instant.now();
        RecoveryOpportunityLog saved = logs.saveAndFlush(new RecoveryOpportunityLog(incident.getIncidentId(),
                properties.maturity(), properties.mode(), candidates, choice,
                fallbackStrategy == null ? null : fallbackStrategy.name(), now));
        return new RecoveryOpportunityDecision(saved.getId(), incident.getIncidentId(), properties.maturity(),
                properties.mode(), candidates, choice, saved.getFallbackStrategy(), now);
    }

    private ActionOpportunity candidate(OpportunityAction action, RevenueIncident incident,
                                        List<HistoricalIncident> historical) {
        BigDecimal priority = BigDecimal.valueOf(Math.min(1.0,
                Math.log10(Math.max(10, incident.getAmountAtRiskMinor())) / 7.0)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal actionProbability = null;
        String estimateKind = "UNAVAILABLE_M0_NO_CAUSAL_MODEL";
        List<String> evidence = new ArrayList<>();
        evidence.add("Capability is registered by Sentinel: " + action);
        if (properties.maturity().ordinal() >= CausalMaturity.M1.ordinal()) {
            RecoveryStrategy strategy = strategy(action);
            if (strategy != null) {
                List<HistoricalIncident> comparable = historical.stream()
                        .filter(item -> item.getRecoveryStrategy() == strategy).toList();
                if (!comparable.isEmpty()) {
                    long recovered = comparable.stream().filter(item -> item.getOutcome() == RecoveryOutcomeStatus.RECOVERED).count();
                    actionProbability = BigDecimal.valueOf((double) recovered / comparable.size())
                            .setScale(4, RoundingMode.HALF_UP);
                    estimateKind = "OBSERVATIONAL_DESCRIPTIVE_NOT_CAUSAL";
                    evidence.add("Historical resolved frequency=" + recovered + "/" + comparable.size());
                }
            }
        }
        evidence.add("Natural recovery is unavailable; uplift and net incremental value are not estimated.");
        return new ActionOpportunity(action, priority, null, actionProbability, null, null,
                "NOT_EVALUATED", "NOT_EVALUATED", estimateKind, evidence);
    }
    private RecoveryStrategy strategy(OpportunityAction action) { return switch (action) {
        case CREATE_PAYMENT_LINK, REQUEST_ALTERNATE_METHOD -> RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK;
        case WAIT_FOR_DOWNTIME_RECOVERY -> RecoveryStrategy.WAIT_FOR_PROVIDER;
        case CUSTOMER_OUTREACH -> RecoveryStrategy.RECOVERY_REMINDER;
        case HUMAN_ESCALATION -> RecoveryStrategy.HUMAN_ESCALATION;
        case NO_ACTION -> RecoveryStrategy.NO_ACTION;
        case CREATE_NEW_ORDER -> null;
    }; }
}
