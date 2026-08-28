package com.sentinel.revenue.metrics;

import com.sentinel.revenue.model.RecoveryStrategy;
import java.math.BigDecimal;

public record StrategyPerformance(RecoveryStrategy strategy, long attemptedRecoveryMinor,
                                  long recoveredRevenueMinor, BigDecimal recoveryRate) { }
