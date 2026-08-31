package com.sentinel.evaluation;

import java.util.List;
import java.util.Map;

public record RecoveryOlympicsReport(
        String title,
        String truthLabel,
        String datasetVersion,
        long seed,
        int datasetSize,
        Map<RecoveryOlympicsSplit, Integer> frozenSplit,
        List<ArmResult> arms,
        List<String> integrityRules,
        List<String> simulatorAssumptions,
        List<String> limitations) {

    public record ArmResult(
            String arm,
            String label,
            String methodologyLabel,
            int sampleCount,
            int interventions,
            int refusals,
            int noActions,
            long grossRecoveryMinor,
            long naturalRecoveryMinor,
            long incrementalRecoveryMinor,
            RateWithInterval incrementalRecoveryRate,
            long recoveryCostMinor,
            long netIncrementalValueMinor,
            double meanTimeToRecoveryMinutes,
            double customerContactRate,
            double humanEscalationRate,
            double falseInterventionRate,
            double unnecessaryInterventionRate,
            int duplicateFinancialEffects,
            int unsafeExecutions,
            int policyViolations,
            double auditCompleteness,
            Latency decisionLatencyMillis) { }

    public record RateWithInterval(double value, double lower95, double upper95,
                                   long numerator, long denominator, String method) { }

    public record Latency(double p50, double p95, double p99,
                          String measurementMode) { }
}
