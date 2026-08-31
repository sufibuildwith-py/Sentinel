package com.sentinel.revenue.opportunity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record RecoveryOpportunityDecision(UUID decisionId, UUID incidentId, CausalMaturity maturity,
                                          String mode, List<ActionOpportunity> candidates,
                                          OpportunityAction shadowChoice, String fallbackStrategy,
                                          Instant createdAt) {
    public RecoveryOpportunityDecision { candidates = List.copyOf(candidates); }
}
