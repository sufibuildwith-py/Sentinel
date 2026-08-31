package com.sentinel.revenue.investigation;

import com.sentinel.revenue.model.AgentClaim;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.repository.AgentClaimRepository;
import org.springframework.stereotype.Service;

@Service
public class AgentClaimService {
    private final AgentClaimRepository claims;
    private final AgentClaimValidator validator;

    public AgentClaimService(AgentClaimRepository claims, AgentClaimValidator validator) {
        this.claims = claims;
        this.validator = validator;
    }

    public AgentClaim validateAndPersist(RevenueIncident incident,
                                         ConsequentialAgentClaim claim,
                                         ClaimValidationContext context) {
        ClaimValidationResult result = validator.validate(claim, context);
        return claims.saveAndFlush(new AgentClaim(incident, claim.claimType(), claim.claim(),
                result.effectiveConfidence(), claim.evidenceRefs(), claim.contradictingEvidenceRefs(),
                claim.proposedAction(), result.status(), result.errors(), claim.createdAt()));
    }
}
