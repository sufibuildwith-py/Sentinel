package com.sentinel.revenue.model;
import com.sentinel.revenue.opportunity.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.*;

@Entity @Table(name = "recovery_opportunity_decisions")
public class RecoveryOpportunityLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "incident_id", nullable = false) private UUID incidentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private CausalMaturity maturity;
    @Column(nullable = false, length = 32) private String mode;
    @Column(name = "feature_schema_version", nullable = false, length = 32) private String featureSchemaVersion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb")
    private List<ActionOpportunity> candidates = new ArrayList<>();
    @Enumerated(EnumType.STRING) @Column(name = "shadow_choice", nullable = false, length = 64)
    private OpportunityAction shadowChoice;
    @Column(name = "fallback_strategy", length = 64) private String fallbackStrategy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected RecoveryOpportunityLog() { }
    public RecoveryOpportunityLog(UUID incidentId, CausalMaturity maturity, String mode,
                                  List<ActionOpportunity> candidates, OpportunityAction choice,
                                  String fallbackStrategy, Instant createdAt) {
        this.incidentId = incidentId; this.maturity = maturity; this.mode = mode;
        this.featureSchemaVersion = "opportunity-v1"; this.candidates = new ArrayList<>(candidates);
        this.shadowChoice = choice; this.fallbackStrategy = fallbackStrategy; this.createdAt = createdAt;
    }
    public UUID getId() { return id; } public UUID getIncidentId() { return incidentId; }
    public CausalMaturity getMaturity() { return maturity; } public String getMode() { return mode; }
    public String getFeatureSchemaVersion() { return featureSchemaVersion; }
    public List<ActionOpportunity> getCandidates() { return List.copyOf(candidates); }
    public OpportunityAction getShadowChoice() { return shadowChoice; }
    public String getFallbackStrategy() { return fallbackStrategy; } public Instant getCreatedAt() { return createdAt; }
}
