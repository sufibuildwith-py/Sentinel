package com.sentinel.revenue.investigation;

import com.sentinel.revenue.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class AgentClaimValidator {
    private static final BigDecimal STALE_CONFIDENCE_FACTOR = new BigDecimal("0.5000");

    public ClaimValidationResult validate(ConsequentialAgentClaim claim,
                                          ClaimValidationContext context) {
        List<String> errors = new ArrayList<>();
        boolean stale = false;
        Map<UUID, IncidentFinding> known = new HashMap<>();
        for (IncidentFinding finding : context.evidence()) {
            if (finding.getId() != null) known.put(finding.getId(), finding);
        }
        for (UUID reference : union(claim.evidenceRefs(), claim.contradictingEvidenceRefs())) {
            IncidentFinding finding = known.get(reference);
            if (finding == null) {
                errors.add("UNKNOWN_EVIDENCE_REFERENCE:" + reference);
            } else if (finding.getValidUntil() != null && finding.getValidUntil().isBefore(context.now())) {
                stale = true;
                errors.add("STALE_EVIDENCE_REFERENCE:" + reference);
            }
        }
        if (claim.claim() == null || claim.claim().isBlank()) errors.add("EMPTY_CLAIM");
        if (claim.confidence() == null || claim.confidence().compareTo(BigDecimal.ZERO) < 0
                || claim.confidence().compareTo(BigDecimal.ONE) > 0) errors.add("INVALID_CONFIDENCE");
        if (claim.proposedAction() != null && !claim.proposedAction().isBlank()
                && !isSupportedStrategy(claim.proposedAction())) errors.add("UNSUPPORTED_ACTION");

        RecoveryAction action = context.action();
        RecoveryOutcome outcome = context.outcome();
        if (claim.claimType() == ClaimType.PROVIDER_STATE && !providerStateExists(action)) {
            errors.add("MISSING_REQUIRED_PROVIDER_STATE");
        }
        if (claim.claimType() == ClaimType.EXECUTION_ASSERTION && !executionExists(action)) {
            errors.add("EXECUTION_CLAIM_WITHOUT_EXECUTION_RECORD");
        }
        if (claim.claimType() == ClaimType.RECOVERY_ASSERTION
                && (outcome == null || !outcome.isProviderConfirmed())) {
            errors.add("RECOVERY_CLAIM_WITHOUT_RECONCILIATION");
        }

        boolean fatal = errors.stream().anyMatch(error -> !error.startsWith("STALE_EVIDENCE_REFERENCE:"));
        if (fatal) return new ClaimValidationResult(ClaimValidationStatus.REJECTED,
                safeConfidence(claim.confidence()), errors);
        if (stale) return new ClaimValidationResult(ClaimValidationStatus.DOWNGRADED,
                safeConfidence(claim.confidence()).multiply(STALE_CONFIDENCE_FACTOR)
                        .setScale(4, RoundingMode.HALF_UP), errors);
        return new ClaimValidationResult(ClaimValidationStatus.VALID,
                safeConfidence(claim.confidence()), List.of());
    }

    private List<UUID> union(List<UUID> first, List<UUID> second) {
        List<UUID> result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }

    private boolean isSupportedStrategy(String value) {
        try { RecoveryStrategy.valueOf(value); return true; }
        catch (IllegalArgumentException ignored) { return false; }
    }

    private boolean providerStateExists(RecoveryAction action) {
        return action != null && action.getExternalResourceId() != null
                && action.getExternalResourceStatus() != null;
    }

    private boolean executionExists(RecoveryAction action) {
        return action != null && action.getExecutedAt() != null
                && action.getExternalResourceId() != null;
    }

    private BigDecimal safeConfidence(BigDecimal confidence) {
        if (confidence == null) return BigDecimal.ZERO.setScale(4);
        return confidence.max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(4, RoundingMode.HALF_UP);
    }
}
