package com.sentinel.revenue.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "agent_claims")
public class AgentClaim {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private RevenueIncident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 32)
    private ClaimType claimType;

    @Column(name = "claim_text", nullable = false, columnDefinition = "text")
    private String claim;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_refs", nullable = false, columnDefinition = "jsonb")
    private List<UUID> evidenceRefs = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contradicting_evidence_refs", nullable = false, columnDefinition = "jsonb")
    private List<UUID> contradictingEvidenceRefs = new ArrayList<>();

    @Column(name = "proposed_action", length = 64)
    private String proposedAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 16)
    private ClaimValidationStatus validationStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_errors", nullable = false, columnDefinition = "jsonb")
    private List<String> validationErrors = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AgentClaim() { }

    public AgentClaim(RevenueIncident incident, ClaimType claimType, String claim,
                      BigDecimal confidence, List<UUID> evidenceRefs,
                      List<UUID> contradictingEvidenceRefs, String proposedAction,
                      ClaimValidationStatus validationStatus, List<String> validationErrors,
                      Instant createdAt) {
        this.incident = incident;
        this.claimType = claimType;
        this.claim = claim;
        this.confidence = confidence;
        this.evidenceRefs = new ArrayList<>(evidenceRefs == null ? List.of() : evidenceRefs);
        this.contradictingEvidenceRefs = new ArrayList<>(
                contradictingEvidenceRefs == null ? List.of() : contradictingEvidenceRefs);
        this.proposedAction = proposedAction;
        this.validationStatus = validationStatus;
        this.validationErrors = new ArrayList<>(validationErrors == null ? List.of() : validationErrors);
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incident.getIncidentId(); }
    public ClaimType getClaimType() { return claimType; }
    public String getClaim() { return claim; }
    public BigDecimal getConfidence() { return confidence; }
    public List<UUID> getEvidenceRefs() { return List.copyOf(evidenceRefs); }
    public List<UUID> getContradictingEvidenceRefs() { return List.copyOf(contradictingEvidenceRefs); }
    public String getProposedAction() { return proposedAction; }
    public ClaimValidationStatus getValidationStatus() { return validationStatus; }
    public List<String> getValidationErrors() { return List.copyOf(validationErrors); }
    public Instant getCreatedAt() { return createdAt; }
}
