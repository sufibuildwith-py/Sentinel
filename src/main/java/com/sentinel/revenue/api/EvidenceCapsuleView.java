package com.sentinel.revenue.api;

import com.sentinel.revenue.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EvidenceCapsuleView(
        UUID incidentId,
        Instant assembledAt,
        List<WebhookEvidence> webhooks,
        IncidentDetailView.TruthView providerTruth,
        List<SystemicEvidence> systemicEvidence,
        List<ClaimEvidence> agentClaims,
        String prediction,
        List<PolicyEvidence> policy,
        ExecutionEvidence execution,
        ReconciliationEvidence reconciliation,
        String finalOutcome,
        Completeness completeness) {
    public record WebhookEvidence(String eventId, String eventType, boolean verified,
                                  boolean processed, Instant receivedAt, Instant processedAt) { }
    public record SystemicEvidence(UUID evidenceId, String source, String summary,
                                   BigDecimal confidence, Instant capturedAt, Instant validUntil,
                                   boolean fresh) { }
    public record ClaimEvidence(UUID claimId, ClaimType claimType, String claim,
                                BigDecimal confidence, List<UUID> evidenceRefs,
                                List<UUID> contradictingEvidenceRefs, String proposedAction,
                                ClaimValidationStatus validationStatus,
                                List<String> validationErrors, Instant createdAt) { }
    public record PolicyEvidence(Instant timestamp, String narrative,
                                 List<String> ruleTrace, String result) { }
    public record ExecutionEvidence(UUID actionId, RecoveryActionStatus status,
                                    PolicyDecision policyDecision, ExecutionMode executionMode,
                                    String providerResourceId, Instant executedAt) { }
    public record ReconciliationEvidence(UUID outcomeId, RecoveryOutcomeStatus status,
                                         long recoveredAmountMinor, boolean providerConfirmed,
                                         String confirmationSource, String sourceEventId,
                                         Instant occurredAt) { }
    public record Completeness(int presentStages, int totalStages, List<String> missingStages) {
        public Completeness { missingStages = List.copyOf(missingStages); }
    }
}
