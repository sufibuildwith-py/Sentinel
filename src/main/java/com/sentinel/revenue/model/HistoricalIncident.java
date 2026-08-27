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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "historical_incidents")
public class HistoricalIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_incident_id")
    private RevenueIncident originalIncident;

    @Column(name = "root_cause", nullable = false, columnDefinition = "text")
    private String rootCause;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_summary", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> evidenceSummary = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "recovery_strategy", nullable = false, length = 64)
    private RecoveryStrategy recoveryStrategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecoveryOutcomeStatus outcome;

    @Column(name = "recovered_amount_minor", nullable = false)
    private long recoveredAmountMinor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HistoricalIncident() {
    }

    public HistoricalIncident(RevenueIncident originalIncident, String rootCause,
                              Map<String, Object> evidenceSummary,
                              RecoveryStrategy recoveryStrategy,
                              RecoveryOutcomeStatus outcome, long recoveredAmountMinor,
                              Instant createdAt) {
        this.originalIncident = originalIncident;
        this.rootCause = rootCause;
        this.evidenceSummary = evidenceSummary == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(evidenceSummary);
        this.recoveryStrategy = recoveryStrategy;
        this.outcome = outcome;
        this.recoveredAmountMinor = recoveredAmountMinor;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getOriginalIncidentId() {
        return originalIncident == null ? null : originalIncident.getIncidentId();
    }
    public String getRootCause() { return rootCause; }
    public Map<String, Object> getEvidenceSummary() { return Map.copyOf(evidenceSummary); }
    public RecoveryStrategy getRecoveryStrategy() { return recoveryStrategy; }
    public RecoveryOutcomeStatus getOutcome() { return outcome; }
    public long getRecoveredAmountMinor() { return recoveredAmountMinor; }
    public Instant getCreatedAt() { return createdAt; }
}
