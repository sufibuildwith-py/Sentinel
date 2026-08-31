package com.sentinel.revenue.metrics;

public record FinancialAttribution(
        String label,
        long failedValueMinor,
        long policyOrProviderIneligibleMinor,
        long addressableValueMinor,
        long expectedNaturalRecoveryMinor,
        String naturalRecoveryEstimationStatus,
        long expectedIncrementalOpportunityMinor,
        long executedValueMinor,
        long providerConfirmedRecoveryMinor,
        long unreconciledExecutedValueMinor,
        long attributedIncrementalRecoveryMinor,
        long recoveryCostMinor,
        String recoveryCostStatus,
        long netIncrementalValueMinor,
        OperationalTimings timings) {
    public record OperationalTimings(TimingMetric ttd, TimingMetric tgd,
                                     TimingMetric tte, TimingMetric ttr) { }
    public record TimingMetric(Long averageMillis, int samples, String definition) { }
}
