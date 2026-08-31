package com.sentinel.revenue.model;

import com.sentinel.revenue.economics.EconomicEvidenceQuality;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DecisionCertificateDraft(
        UUID decisionId, UUID incidentId, UUID recoveryActionId, String decisionType,
        String policyVersion, String modelVersion, String featureSchemaVersion, String strategyVersion,
        String inputSnapshotHash, String evidenceCapsuleHash, List<String> candidateActions,
        List<String> rejectedAlternatives, String selectedAction, String counterfactualMethod,
        EconomicEvidenceQuality evidenceQuality, BigDecimal expectedIncrementalValueMinor,
        String authorizationResult, String exposureDecision, String executionReference,
        String providerReference, String reconciliationReference, String attributionReference,
        String finalTruthState, String certificateVersion) {
    public DecisionCertificateDraft {
        candidateActions = List.copyOf(candidateActions);
        rejectedAlternatives = List.copyOf(rejectedAlternatives);
    }
}
