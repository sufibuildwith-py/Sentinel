package com.sentinel.revenue.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "incident_findings")
public class IncidentFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private RevenueIncident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FindingSource source;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> evidence = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IncidentFinding() {
    }

    public IncidentFinding(RevenueIncident incident, FindingSource source, String summary,
                           BigDecimal confidence, List<String> evidence, Instant createdAt) {
        this.incident = incident;
        this.source = source;
        this.summary = summary;
        this.confidence = confidence;
        this.evidence = evidence == null ? new ArrayList<>() : new ArrayList<>(evidence);
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incident.getIncidentId(); }
    public FindingSource getSource() { return source; }
    public String getSummary() { return summary; }
    public BigDecimal getConfidence() { return confidence; }
    public List<String> getEvidence() { return List.copyOf(evidence); }
    public Instant getCreatedAt() { return createdAt; }
}
