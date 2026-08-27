package com.sentinel.revenue.detection;

import java.util.Map;

public record PaymentStatistics(
        int eventCount,
        int failedEventCount,
        long totalValueMinor,
        long amountAtRiskMinor,
        double overallSuccessRate,
        Map<String, Double> successRateByMethod,
        Map<String, Double> successRateByIssuer,
        Map<String, Long> failureCodeDistribution,
        double retryFrequency,
        double valueConcentration,
        double abandonmentRate,
        double baselineSuccessRate,
        double baselineStandardDeviation,
        double baselineDeviation,
        int baselineBucketCount) {

    public PaymentStatistics {
        successRateByMethod = Map.copyOf(successRateByMethod);
        successRateByIssuer = Map.copyOf(successRateByIssuer);
        failureCodeDistribution = Map.copyOf(failureCodeDistribution);
    }

    public double successRateDrop() {
        return Math.max(0.0, baselineSuccessRate - overallSuccessRate);
    }
}
