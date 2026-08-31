package com.sentinel.revenue.experiment;

import com.sentinel.revenue.opportunity.OpportunityAction;

import java.math.BigDecimal;

public record RegretCandidate(OpportunityAction action, BigDecimal estimatedNetIncrementalValueMinor,
                              boolean alreadyPolicyApproved, String evidenceQuality) { }
