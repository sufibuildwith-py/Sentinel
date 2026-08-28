package com.sentinel.revenue.metrics;

import java.math.BigDecimal;
import java.util.List;

public record RevenueMetrics(String label, String mode, long revenueAtRiskMinor,
                             long attemptedRecoveryMinor, long recoveredRevenueMinor,
                             BigDecimal recoveryRate, List<StrategyPerformance> strategyPerformance) {
    public RevenueMetrics { strategyPerformance = List.copyOf(strategyPerformance); }
}
