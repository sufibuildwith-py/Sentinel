package com.sentinel.revenue.controltower;

import com.sentinel.revenue.health.PaymentHealthReport;
import com.sentinel.revenue.metrics.FinancialAttribution;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ControlTowerView(
        String scopeLabel,
        Instant generatedAt,
        PaymentHealthReport paymentHealth,
        FinancialAttribution financialAttribution,
        List<SystemicIncidentView> systemicIncidents,
        List<OpportunityView> opportunities,
        GovernorPosture governor,
        List<ModelView> models,
        ShadowPosture replayAndShadow,
        PromisePosture promises,
        List<String> truthLabels) {

    public ControlTowerView {
        systemicIncidents = List.copyOf(systemicIncidents);
        opportunities = List.copyOf(opportunities);
        models = List.copyOf(models);
        truthLabels = List.copyOf(truthLabels);
    }

    public record SystemicIncidentView(UUID id, String status, String scope,
                                       List<RootCauseView> rootCauses, Instant createdAt) {
        public SystemicIncidentView { rootCauses = List.copyOf(rootCauses); }
    }

    public record RootCauseView(String cause, double confidence, String scope,
                                List<String> support, List<String> contradiction) {
        public RootCauseView {
            support = List.copyOf(support);
            contradiction = List.copyOf(contradiction);
        }
    }

    public record OpportunityView(UUID decisionId, UUID incidentId, String maturity,
                                  String mode, String selectedAction, String fallbackStrategy,
                                  BigDecimal priorityScore, String policyState,
                                  String governorState, Long netIncrementalValueMinor,
                                  Instant createdAt) { }

    public record GovernorPosture(Map<String, Boolean> killSwitches,
                                  long maxTotalValueMinor, long maxValuePerIncidentMinor,
                                  int maxIncidents, int maxProviderCallsPerMinute,
                                  int maxConcurrentJobs, long maxUnreconciledValueMinor,
                                  int canarySize, int requiredReconciledCount,
                                  List<BatchView> batches) {
        public GovernorPosture {
            killSwitches = Map.copyOf(killSwitches);
            batches = List.copyOf(batches);
        }
    }

    public record BatchView(UUID id, String strategy, String status, int incidentCount,
                            int releasedCount, int requiredReconciledCount) { }

    public record ModelView(UUID id, String name, String version,
                            String featureSchemaVersion, String lifecycle, Instant createdAt) { }

    public record ShadowPosture(long snapshotCount, long comparisonCount,
                                long criticalRegressionCount,
                                List<ShadowDifferenceView> latestDifferences) {
        public ShadowPosture { latestDifferences = List.copyOf(latestDifferences); }
    }

    public record ShadowDifferenceView(UUID id, String productionAction, String shadowAction,
                                       String productionPolicy, String shadowPolicy,
                                       String productionGovernor, String shadowGovernor,
                                       boolean rankingChanged, boolean approvalRequirementChanged,
                                       boolean criticalRegression, String explanation, Instant createdAt) { }

    public record PromisePosture(long total, Map<String, Long> byStatus,
                                 long promisedAmountMinor, long fulfilledAmountMinor) {
        public PromisePosture { byStatus = Map.copyOf(byStatus); }
    }
}
