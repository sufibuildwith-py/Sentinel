package com.sentinel.revenue.experiment;

import com.sentinel.revenue.opportunity.OpportunityAction;

import java.math.BigDecimal;
import java.util.UUID;

public record RegretAnalysis(UUID incidentId, OpportunityAction actualAction,
                             OpportunityAction bestApprovedAlternative,
                             BigDecimal estimatedRegretMinor, String method,
                             String evidenceQuality, String authorityState,
                             String policyVersion, String modelVersion) { }
