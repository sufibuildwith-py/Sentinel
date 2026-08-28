package com.sentinel.evaluation;

import com.sentinel.revenue.model.PolicyDecision;

import java.util.List;
import java.util.Map;

public record EvaluationReport(
        String title,
        String scopeLabel,
        String reportVersion,
        long seed,
        int datasetSize,
        String deterministicTimestamp,
        Score detectionPrecision,
        Score detectionRecall,
        Score detectionF1,
        Score rootCauseExactAccuracy,
        Score rootCauseCategoryAccuracy,
        Score policyCompliance,
        Score executionEligibilityAccuracy,
        Score falsePositiveRate,
        Score falseInterventionRate,
        Score escalationRate,
        Score recoveryAttemptRate,
        Score verifiedRecoveryRate,
        ConfusionMatrix detectionConfusionMatrix,
        RecoveryFunnel recoveryFunnel,
        long recoveredAmountMinor,
        int duplicateActionsCreated,
        int duplicateFinancialEffects,
        List<StrategyResult> strategyPerformance,
        Map<String, LatencyResult> latencyMillis,
        List<SafetyGate> safetyGates,
        List<ScenarioResult> scenarios,
        List<FailureInjection> failureInjectionMatrix,
        List<MetricDefinition> metricDefinitions,
        List<String> limitations) {

    public record Score(long numerator, long denominator, double value) { }
    public record ConfusionMatrix(int truePositive, int falsePositive, int falseNegative, int trueNegative) { }
    public record RecoveryFunnel(long amountAtRiskMinor, int detectedIncidents,
                                 int policyEligible, int attempted, int verifiedRecovered) { }
    public record StrategyResult(String strategy, int sampleCount, int attemptedCount,
                                 int recoveredCount, long attemptedAmountMinor,
                                 long recoveredAmountMinor, double recoveryRate) { }
    public record LatencyResult(int sampleCount, double p50, double p95,
                                String measurementMode) { }
    public record SafetyGate(String gate, long actual, String required, boolean passed,
                             String evidence) { }
    public record ScenarioResult(String scenarioId, String category,
                                 boolean expectedIncident, boolean actualIncident,
                                 String expectedRootCauseCategory, String actualRootCauseCategory,
                                 PolicyDecision expectedPolicyDecision, PolicyDecision actualPolicyDecision,
                                 boolean approvalRequired, String expectedExecutionBehavior,
                                 String actualExecutionBehavior, String expectedProviderOutcome,
                                 String actualProviderOutcome, long expectedFinancialMutationMinor,
                                 long actualFinancialMutationMinor, boolean passed,
                                 List<String> auditEvents, Map<String, Integer> logicalLatencyMillis) { }
    public record FailureInjection(String failure, int scenarioCount, String observedBehavior,
                                   boolean bounded, String evidence) { }
    public record MetricDefinition(String metric, String formula, long numerator,
                                   long denominator, String evidence) { }
}
