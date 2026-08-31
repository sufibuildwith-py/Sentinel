package com.sentinel.revenue.experiment;

import com.sentinel.revenue.economics.EconomicEvidenceQuality;

import java.util.List;

public record ExperimentSummary(UUIDVersion experiment, List<ArmResult> arms,
                                boolean minimumSampleReached, boolean harmThresholdBreached,
                                EconomicEvidenceQuality evidenceQuality, String conclusion,
                                String authorityState) {
    public ExperimentSummary { arms = List.copyOf(arms); }
    public record UUIDVersion(String experimentId, String policyVersion, String modelVersion, long seed) { }
    public record ArmResult(String arm, boolean control, int samples, int confirmedRecoveries,
                            long grossRecoveredMinor, long recoveryCostMinor,
                            long netRecoveredAfterCostMinor, Double recoveryRate,
                            Double harmRate, Long averageTimeToRecoveryMillis) { }
}
