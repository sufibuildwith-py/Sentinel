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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recovery_plans")
public class RecoveryPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private RevenueIncident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private RecoveryStrategy strategy;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "target_payment_count", nullable = false)
    private int targetPaymentCount;

    @Column(name = "target_amount_minor", nullable = false)
    private long targetAmountMinor;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "estimated_recovery_minor", nullable = false)
    private long estimatedRecoveryMinor;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RecoveryPlan() {
    }

    public RecoveryPlan(RevenueIncident incident, RecoveryStrategy strategy, String reason,
                        int targetPaymentCount, long targetAmountMinor, BigDecimal confidence,
                        long estimatedRecoveryMinor, RiskLevel riskLevel, Instant createdAt) {
        this.incident = incident;
        this.strategy = strategy;
        this.reason = reason;
        this.targetPaymentCount = targetPaymentCount;
        this.targetAmountMinor = targetAmountMinor;
        this.confidence = confidence;
        this.estimatedRecoveryMinor = estimatedRecoveryMinor;
        this.riskLevel = riskLevel;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incident.getIncidentId(); }
    public RecoveryStrategy getStrategy() { return strategy; }
    public String getReason() { return reason; }
    public int getTargetPaymentCount() { return targetPaymentCount; }
    public long getTargetAmountMinor() { return targetAmountMinor; }
    public BigDecimal getConfidence() { return confidence; }
    public long getEstimatedRecoveryMinor() { return estimatedRecoveryMinor; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public Instant getCreatedAt() { return createdAt; }
}
