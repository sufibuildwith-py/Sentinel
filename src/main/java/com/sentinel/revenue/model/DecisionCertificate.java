package com.sentinel.revenue.model;

import com.sentinel.revenue.economics.EconomicEvidenceQuality;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "decision_certificates")
public class DecisionCertificate {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "decision_id", nullable = false, unique = true) private UUID decisionId;
    @Column(name = "incident_id", nullable = false) private UUID incidentId;
    @Column(name = "recovery_action_id") private UUID recoveryActionId;
    @Column(name = "decision_type", nullable = false, length = 64) private String decisionType;
    @Column(name = "policy_version", nullable = false, length = 64) private String policyVersion;
    @Column(name = "model_version", nullable = false, length = 64) private String modelVersion;
    @Column(name = "feature_schema_version", nullable = false, length = 64) private String featureSchemaVersion;
    @Column(name = "strategy_version", nullable = false, length = 64) private String strategyVersion;
    @Column(name = "input_snapshot_hash", nullable = false, length = 64) private String inputSnapshotHash;
    @Column(name = "evidence_capsule_hash", length = 64) private String evidenceCapsuleHash;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "candidate_actions", nullable = false, columnDefinition = "jsonb")
    private List<String> candidateActions = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "rejected_alternatives", nullable = false, columnDefinition = "jsonb")
    private List<String> rejectedAlternatives = new ArrayList<>();
    @Column(name = "selected_action", nullable = false, length = 64) private String selectedAction;
    @Column(name = "counterfactual_method", nullable = false, length = 64) private String counterfactualMethod;
    @Enumerated(EnumType.STRING) @Column(name = "evidence_quality", nullable = false, length = 32)
    private EconomicEvidenceQuality evidenceQuality;
    @Column(name = "expected_incremental_value_minor", precision = 19, scale = 0)
    private BigDecimal expectedIncrementalValueMinor;
    @Column(name = "authorization_result", nullable = false, length = 64) private String authorizationResult;
    @Column(name = "exposure_decision", nullable = false, length = 64) private String exposureDecision;
    @Column(name = "execution_reference", length = 128) private String executionReference;
    @Column(name = "provider_reference", length = 128) private String providerReference;
    @Column(name = "reconciliation_reference", length = 128) private String reconciliationReference;
    @Column(name = "attribution_reference", length = 128) private String attributionReference;
    @Column(name = "final_truth_state", nullable = false, length = 64) private String finalTruthState;
    @Column(name = "certificate_version", nullable = false, length = 32) private String certificateVersion;
    @Column(name = "certificate_sha256", nullable = false, unique = true, length = 64) private String certificateSha256;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected DecisionCertificate() { }

    public DecisionCertificate(DecisionCertificateDraft draft, String certificateSha256, Instant createdAt) {
        this.decisionId = draft.decisionId(); this.incidentId = draft.incidentId();
        this.recoveryActionId = draft.recoveryActionId(); this.decisionType = draft.decisionType();
        this.policyVersion = draft.policyVersion(); this.modelVersion = draft.modelVersion();
        this.featureSchemaVersion = draft.featureSchemaVersion(); this.strategyVersion = draft.strategyVersion();
        this.inputSnapshotHash = draft.inputSnapshotHash(); this.evidenceCapsuleHash = draft.evidenceCapsuleHash();
        this.candidateActions = new ArrayList<>(draft.candidateActions());
        this.rejectedAlternatives = new ArrayList<>(draft.rejectedAlternatives());
        this.selectedAction = draft.selectedAction(); this.counterfactualMethod = draft.counterfactualMethod();
        this.evidenceQuality = draft.evidenceQuality();
        this.expectedIncrementalValueMinor = draft.expectedIncrementalValueMinor();
        this.authorizationResult = draft.authorizationResult(); this.exposureDecision = draft.exposureDecision();
        this.executionReference = draft.executionReference(); this.providerReference = draft.providerReference();
        this.reconciliationReference = draft.reconciliationReference(); this.attributionReference = draft.attributionReference();
        this.finalTruthState = draft.finalTruthState(); this.certificateVersion = draft.certificateVersion();
        this.certificateSha256 = certificateSha256; this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getDecisionId() { return decisionId; }
    public UUID getIncidentId() { return incidentId; }
    public UUID getRecoveryActionId() { return recoveryActionId; }
    public String getDecisionType() { return decisionType; }
    public String getPolicyVersion() { return policyVersion; }
    public String getModelVersion() { return modelVersion; }
    public String getFeatureSchemaVersion() { return featureSchemaVersion; }
    public String getStrategyVersion() { return strategyVersion; }
    public String getInputSnapshotHash() { return inputSnapshotHash; }
    public String getEvidenceCapsuleHash() { return evidenceCapsuleHash; }
    public List<String> getCandidateActions() { return List.copyOf(candidateActions); }
    public List<String> getRejectedAlternatives() { return List.copyOf(rejectedAlternatives); }
    public String getSelectedAction() { return selectedAction; }
    public String getCounterfactualMethod() { return counterfactualMethod; }
    public EconomicEvidenceQuality getEvidenceQuality() { return evidenceQuality; }
    public BigDecimal getExpectedIncrementalValueMinor() { return expectedIncrementalValueMinor; }
    public String getAuthorizationResult() { return authorizationResult; }
    public String getExposureDecision() { return exposureDecision; }
    public String getExecutionReference() { return executionReference; }
    public String getProviderReference() { return providerReference; }
    public String getReconciliationReference() { return reconciliationReference; }
    public String getAttributionReference() { return attributionReference; }
    public String getFinalTruthState() { return finalTruthState; }
    public String getCertificateVersion() { return certificateVersion; }
    public String getCertificateSha256() { return certificateSha256; }
    public Instant getCreatedAt() { return createdAt; }
}
