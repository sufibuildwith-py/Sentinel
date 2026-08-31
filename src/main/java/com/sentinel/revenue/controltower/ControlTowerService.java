package com.sentinel.revenue.controltower;

import com.sentinel.revenue.governor.KillSwitchService;
import com.sentinel.revenue.governor.RecoverySafetyProperties;
import com.sentinel.revenue.health.PaymentHealthAnalyzer;
import com.sentinel.revenue.metrics.FinancialAttributionService;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.opportunity.ActionOpportunity;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ControlTowerService {
    private static final int LATEST_LIMIT = 12;

    private final PaymentHealthAnalyzer health;
    private final FinancialAttributionService financial;
    private final SystemicRecoveryIncidentRepository systemic;
    private final RecoveryOpportunityLogRepository opportunities;
    private final KillSwitchService killSwitches;
    private final RecoverySafetyProperties safety;
    private final RecoveryBatchRepository batches;
    private final RegisteredModelRepository models;
    private final PolicyReplaySnapshotRepository snapshots;
    private final ShadowDecisionDifferenceRepository shadowDifferences;
    private final PromiseToPayRepository promises;
    private final Clock clock;

    public ControlTowerService(PaymentHealthAnalyzer health, FinancialAttributionService financial,
                               SystemicRecoveryIncidentRepository systemic,
                               RecoveryOpportunityLogRepository opportunities,
                               KillSwitchService killSwitches, RecoverySafetyProperties safety,
                               RecoveryBatchRepository batches, RegisteredModelRepository models,
                               PolicyReplaySnapshotRepository snapshots,
                               ShadowDecisionDifferenceRepository shadowDifferences,
                               PromiseToPayRepository promises, Clock clock) {
        this.health = health;
        this.financial = financial;
        this.systemic = systemic;
        this.opportunities = opportunities;
        this.killSwitches = killSwitches;
        this.safety = safety;
        this.batches = batches;
        this.models = models;
        this.snapshots = snapshots;
        this.shadowDifferences = shadowDifferences;
        this.promises = promises;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ControlTowerView view() {
        var now = clock.instant();
        var systemicViews = systemic.findAll().stream()
                .sorted(Comparator.comparing(SystemicRecoveryIncident::getCreatedAt).reversed())
                .limit(LATEST_LIMIT).map(this::systemicView).toList();
        var opportunityViews = opportunities.findAll().stream()
                .sorted(Comparator.comparing(RecoveryOpportunityLog::getCreatedAt).reversed())
                .limit(LATEST_LIMIT).map(this::opportunityView).toList();
        var batchViews = batches.findAll().stream()
                .sorted(Comparator.comparing(RecoveryBatch::getCreatedAt).reversed())
                .limit(LATEST_LIMIT).map(batch -> new ControlTowerView.BatchView(batch.getId(),
                        batch.getStrategy(), batch.getStatus(), batch.getIncidentIds().size(),
                        batch.getReleasedCount(), batch.getRequiredReconciledCount())).toList();
        var modelViews = models.findAll().stream().map(model -> new ControlTowerView.ModelView(
                model.getId(), model.getModelName(), model.getModelVersion(),
                model.getFeatureSchemaVersion(), model.getLifecycle().name(), model.getCreatedAt())).toList();
        var allDifferences = shadowDifferences.findAll();
        var latestDifferences = allDifferences.stream()
                .sorted(Comparator.comparing(ShadowDecisionDifference::getCreatedAt).reversed())
                .limit(LATEST_LIMIT).map(this::shadowView).toList();
        var allPromises = promises.findAll();
        var promiseStatuses = allPromises.stream().collect(Collectors.groupingBy(
                promise -> promise.getStatus().name(), TreeMap::new, Collectors.counting()));
        Map<String, Boolean> switches = killSwitches.states().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue,
                        (left, right) -> left, TreeMap::new));

        return new ControlTowerView("RAZORPAY TEST MODE / SYNTHETIC EVALUATION", now,
                health.analyze("ALL", now), financial.attribution(), systemicViews, opportunityViews,
                new ControlTowerView.GovernorPosture(switches, safety.maxTotalValueMinor(),
                        safety.maxValuePerIncidentMinor(), safety.maxIncidents(),
                        safety.maxProviderCallsPerMinute(), safety.maxConcurrentJobs(),
                        safety.maxUnreconciledValueMinor(), safety.canarySize(),
                        safety.requiredReconciledCount(), batchViews),
                modelViews, new ControlTowerView.ShadowPosture(snapshots.count(), allDifferences.size(),
                        allDifferences.stream().filter(ShadowDecisionDifference::isCriticalRegression).count(),
                        latestDifferences),
                new ControlTowerView.PromisePosture(allPromises.size(), promiseStatuses,
                        allPromises.stream().mapToLong(PromiseToPay::getPromisedAmountMinor).sum(),
                        allPromises.stream().mapToLong(PromiseToPay::getFulfilledAmountMinor).sum()),
                List.of("RAZORPAY TEST MODE", "SIMULATION", "FAULT INJECTION",
                        "SYNTHETIC BENCHMARK", "SHADOW ONLY", "PROVIDER CONFIRMED",
                        "AWAITING RECONCILIATION"));
    }

    private ControlTowerView.SystemicIncidentView systemicView(SystemicRecoveryIncident incident) {
        var rootCauses = incident.getRootCauseCandidates().stream().map(candidate ->
                new ControlTowerView.RootCauseView(candidate.cause(), candidate.confidence(),
                        candidate.scope(), candidate.support(), candidate.contradiction())).toList();
        return new ControlTowerView.SystemicIncidentView(incident.getId(), incident.getStatus(),
                incident.getScope(), rootCauses, incident.getCreatedAt());
    }

    private ControlTowerView.OpportunityView opportunityView(RecoveryOpportunityLog log) {
        ActionOpportunity selected = log.getCandidates().stream()
                .filter(candidate -> candidate.action() == log.getShadowChoice()).findFirst().orElse(null);
        return new ControlTowerView.OpportunityView(log.getId(), log.getIncidentId(),
                log.getMaturity().name(), log.getMode(), log.getShadowChoice().name(),
                log.getFallbackStrategy(), selected == null ? null : selected.priorityScore(),
                selected == null ? null : selected.policyState(),
                selected == null ? null : selected.governorState(),
                selected == null ? null : selected.netIncrementalValueMinor(), log.getCreatedAt());
    }

    private ControlTowerView.ShadowDifferenceView shadowView(ShadowDecisionDifference difference) {
        return new ControlTowerView.ShadowDifferenceView(difference.getId(),
                difference.getProductionAction(), difference.getShadowAction(),
                difference.getProductionPolicyResult().name(), difference.getShadowPolicyResult().name(),
                difference.getProductionGovernorResult(), difference.getShadowGovernorResult(),
                difference.isOpportunityRankingChanged(), difference.isApprovalRequirementChanged(),
                difference.isCriticalRegression(), difference.getExplanation(), difference.getCreatedAt());
    }
}
