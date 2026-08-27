package com.sentinel.revenue.investigation;

import com.sentinel.revenue.detection.PaymentStatistics;

import java.util.List;

public record PatternAnalysis(PaymentStatistics statistics, List<String> evidence,
                              double dominantFailureShare) {
    public PatternAnalysis { evidence = List.copyOf(evidence); }
}
