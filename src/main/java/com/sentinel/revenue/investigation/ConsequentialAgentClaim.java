package com.sentinel.revenue.investigation;

import com.sentinel.revenue.model.ClaimType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConsequentialAgentClaim(UUID claimId, ClaimType claimType, String claim,
                                      BigDecimal confidence, List<UUID> evidenceRefs,
                                      List<UUID> contradictingEvidenceRefs,
                                      String proposedAction, Instant createdAt) {
    public ConsequentialAgentClaim {
        evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs);
        contradictingEvidenceRefs = List.copyOf(
                contradictingEvidenceRefs == null ? List.of() : contradictingEvidenceRefs);
    }
}
