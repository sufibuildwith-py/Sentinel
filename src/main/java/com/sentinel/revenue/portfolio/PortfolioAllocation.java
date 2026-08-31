package com.sentinel.revenue.portfolio;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioAllocation(int totalCasesConsidered, long totalRevenueAtRiskMinor,
                                  List<PortfolioCandidate> selected, int casesNoAction,
                                  int casesPolicyBlocked, int casesGovernorBlocked,
                                  int casesHumanReview, long allocatedExposureMinor,
                                  int allocatedContacts, BigDecimal expectedNetIncrementalValueMinor,
                                  BigDecimal unallocatedOpportunityMinor, String bindingConstraint,
                                  String authorityState) {
    public PortfolioAllocation { selected = List.copyOf(selected); }
}
