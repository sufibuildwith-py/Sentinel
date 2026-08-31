package com.sentinel.revenue.model;

import com.sentinel.revenue.economics.EconomicEvidenceQuality;
import com.sentinel.revenue.economics.RecoveryCostCategory;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recovery_cost_entries")
public class RecoveryCostEntry {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "incident_id", nullable = false) private UUID incidentId;
    @Column(name = "recovery_action_id") private UUID recoveryActionId;
    @Column(name = "decision_id") private UUID decisionId;
    @Enumerated(EnumType.STRING) @Column(name = "cost_category", nullable = false, length = 64)
    private RecoveryCostCategory costCategory;
    @Column(name = "amount_minor", nullable = false, precision = 19, scale = 0)
    private BigDecimal amountMinor;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 128) private String source;
    @Column(name = "calculation_method", nullable = false, length = 128) private String calculationMethod;
    @Enumerated(EnumType.STRING) @Column(name = "evidence_quality", nullable = false, length = 32)
    private EconomicEvidenceQuality evidenceQuality;
    @Column(name = "cost_version", nullable = false, length = 64) private String costVersion;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected RecoveryCostEntry() { }

    public RecoveryCostEntry(UUID incidentId, UUID recoveryActionId, UUID decisionId,
                             RecoveryCostCategory costCategory, BigDecimal amountMinor,
                             String currency, String source, String calculationMethod,
                             EconomicEvidenceQuality evidenceQuality, String costVersion,
                             Instant occurredAt, Instant createdAt) {
        if (incidentId == null) throw new IllegalArgumentException("incidentId is required");
        if (amountMinor == null || amountMinor.signum() < 0) {
            throw new IllegalArgumentException("amountMinor must be non-negative");
        }
        this.incidentId = incidentId;
        this.recoveryActionId = recoveryActionId;
        this.decisionId = decisionId;
        this.costCategory = costCategory;
        this.amountMinor = amountMinor.setScale(0, RoundingMode.UNNECESSARY);
        this.currency = currency;
        this.source = source;
        this.calculationMethod = calculationMethod;
        this.evidenceQuality = evidenceQuality;
        this.costVersion = costVersion;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public UUID getRecoveryActionId() { return recoveryActionId; }
    public UUID getDecisionId() { return decisionId; }
    public RecoveryCostCategory getCostCategory() { return costCategory; }
    public BigDecimal getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public String getSource() { return source; }
    public String getCalculationMethod() { return calculationMethod; }
    public EconomicEvidenceQuality getEvidenceQuality() { return evidenceQuality; }
    public String getCostVersion() { return costVersion; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
}
