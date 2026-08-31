package com.sentinel.revenue.metrics;

import java.util.List;

public record LostRevenueExplorer(
        String label,
        long revenueAtRiskMinor,
        long providerConfirmedRecoveryMinor,
        long unrecoveredMinor,
        List<Reason> reasons,
        String evidenceQuality,
        List<String> limitations) {
    public LostRevenueExplorer { reasons = List.copyOf(reasons); limitations = List.copyOf(limitations); }
    public record Reason(String category, long amountMinor, int incidentCount,
                         String evidenceClass, String explanation) { }
}
