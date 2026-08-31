package com.sentinel.revenue.portfolio;

public record PortfolioConstraints(long exposureBudgetMinor, int contactBudget,
                                   int humanReviewCapacity, int maximumHighRiskActions) {
    public PortfolioConstraints {
        if (exposureBudgetMinor < 0 || contactBudget < 0 || humanReviewCapacity < 0
                || maximumHighRiskActions < 0) throw new IllegalArgumentException("Portfolio constraints cannot be negative");
    }
}
