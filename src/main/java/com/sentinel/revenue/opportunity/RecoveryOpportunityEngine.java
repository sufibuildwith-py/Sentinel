package com.sentinel.revenue.opportunity;

import com.sentinel.revenue.economics.DecisionCertificateService;
import com.sentinel.revenue.economics.EconomicEvidenceQuality;
import com.sentinel.revenue.economics.CounterfactualEstimate;
import com.sentinel.revenue.economics.CounterfactualRecoveryEngine;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.Instant;
import java.time.Clock;
import java.util.*;

@Service
public class RecoveryOpportunityEngine {
    private final ProviderCapabilityRegistry capabilities;
    private final RecoveryOpportunityLogRepository logs;
    private final OpportunityProperties properties;
    private final DecisionCertificateService certificates;
    private final Clock clock;
    private final CounterfactualRecoveryEngine counterfactuals;
    public RecoveryOpportunityEngine(ProviderCapabilityRegistry capabilities,
                                     RecoveryOpportunityLogRepository logs,
                                     OpportunityProperties properties,
                                     DecisionCertificateService certificates,
                                     Clock clock,
                                     CounterfactualRecoveryEngine counterfactuals) {
        this.capabilities = capabilities; this.logs = logs; this.properties = properties;
        this.certificates = certificates; this.clock = clock; this.counterfactuals = counterfactuals;
    }
    @Transactional
    public RecoveryOpportunityDecision evaluate(RevenueIncident incident, RecoveryStrategy fallbackStrategy) {
        List<ActionOpportunity> candidates = capabilities.supportedActions().stream()
                .sorted(Comparator.comparing(Enum::name)).map(action -> candidate(action, incident)).toList();
        OpportunityAction choice = candidates.stream().filter(candidate -> candidate.actionRecoveryProbability() != null)
                .max(Comparator.comparing(ActionOpportunity::actionRecoveryProbability))
                .map(ActionOpportunity::action).orElse(OpportunityAction.NO_ACTION);
        Instant now = clock.instant();
        RecoveryOpportunityLog saved = logs.saveAndFlush(new RecoveryOpportunityLog(incident.getIncidentId(),
                properties.maturity(), properties.mode(), candidates, choice,
                fallbackStrategy == null ? null : fallbackStrategy.name(), now));
        if (saved.getId() != null) issueShadowCertificate(incident, saved, candidates, choice);
        return new RecoveryOpportunityDecision(saved.getId(), incident.getIncidentId(), properties.maturity(),
                properties.mode(), candidates, choice, saved.getFallbackStrategy(), now);
    }

    private void issueShadowCertificate(RevenueIncident incident, RecoveryOpportunityLog saved,
                                        List<ActionOpportunity> candidates, OpportunityAction choice) {
        List<String> candidateNames = candidates.stream().map(candidate -> candidate.action().name()).toList();
        List<String> rejected = candidates.stream().filter(candidate -> candidate.action() != choice)
                .map(candidate -> candidate.action().name() + ": " + candidate.estimateKind()).toList();
        ActionOpportunity selected = candidates.stream().filter(candidate -> candidate.action() == choice)
                .findFirst().orElseThrow();
        String snapshot = incident.getIncidentId() + "|" + incident.getType() + "|"
                + incident.getAmountAtRiskMinor() + "|" + saved.getFeatureSchemaVersion() + "|" + candidateNames;
        EconomicEvidenceQuality quality = properties.maturity() == CausalMaturity.M0
                ? EconomicEvidenceQuality.NOT_ESTIMATED : EconomicEvidenceQuality.OBSERVATIONAL;
        certificates.issue(new DecisionCertificateDraft(saved.getId(), incident.getIncidentId(), null,
                "RECOVERY_OPPORTUNITY_SHADOW", "policy-v1", "none-deterministic",
                saved.getFeatureSchemaVersion(), "strategy-v1", DecisionCertificateService.hashText(snapshot),
                null, candidateNames, rejected, choice.name(), selected.estimateKind(), quality,
                selected.netIncrementalValueMinor() == null ? null : BigDecimal.valueOf(selected.netIncrementalValueMinor()),
                "SHADOW_ONLY_NOT_AUTHORIZED", "NOT_EVALUATED", null, null, null,
                "recovery-opportunity:" + saved.getId(), "SHADOW_ONLY_NO_EXECUTION", "decision-certificate-v1"));
    }

    private ActionOpportunity candidate(OpportunityAction action, RevenueIncident incident) {
        BigDecimal priority = BigDecimal.valueOf(Math.min(1.0,
                Math.log10(Math.max(10, incident.getAmountAtRiskMinor())) / 7.0)).setScale(4, RoundingMode.HALF_UP);
        CounterfactualEstimate estimate = counterfactuals.estimate(incident, action, properties.maturity());
        List<String> evidence = new ArrayList<>();
        evidence.add("Capability is registered by Sentinel: " + action);
        evidence.add("Counterfactual method=" + estimate.method() + ", evidence=" + estimate.evidenceQuality()
                + ", naturalSample=" + estimate.naturalSampleSize() + ", actionSample=" + estimate.actionSampleSize());
        if (estimate.estimatedIncrementalRecoveryMinor() == null) {
            evidence.add("Causal uplift and net incremental value are not estimated at this maturity.");
        }
        return new ActionOpportunity(action, priority, estimate.naturalRecoveryProbability(),
                estimate.actionRecoveryProbability(), null, null, "NOT_EVALUATED", "NOT_EVALUATED",
                estimate.method(), evidence);
    }
}
