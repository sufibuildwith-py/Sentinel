package com.sentinel.revenue.portfolio;

import com.sentinel.revenue.opportunity.OpportunityAction;
import com.sentinel.revenue.opportunity.RecoveryRiskClass;

import java.math.BigDecimal;
import java.util.UUID;

public record PortfolioCandidate(UUID incidentId, OpportunityAction action, long amountMinor,
                                 BigDecimal expectedNetIncrementalValueMinor,
                                 RecoveryRiskClass riskClass, boolean customerContact,
                                 boolean humanReview, boolean policyAdmissible,
                                 boolean governorAdmissible, String evidenceQuality) { }
