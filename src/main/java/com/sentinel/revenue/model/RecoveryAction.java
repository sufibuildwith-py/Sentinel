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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recovery_actions")
public class RecoveryAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recovery_plan_id", nullable = false)
    private RecoveryPlan recoveryPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private RevenueIncident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecoveryActionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_decision", length = 16)
    private PolicyDecision policyDecision;

    @Column(name = "external_resource_type", length = 64)
    private String externalResourceType;

    @Column(name = "external_resource_id", length = 128)
    private String externalResourceId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    protected RecoveryAction() {
    }

    public RecoveryAction(RecoveryPlan recoveryPlan, RevenueIncident incident,
                          RecoveryActionStatus status, PolicyDecision policyDecision,
                          String externalResourceType, String externalResourceId,
                          long amountMinor, Instant createdAt, Instant approvedAt,
                          Instant executedAt) {
        this.recoveryPlan = recoveryPlan;
        this.incident = incident;
        this.status = status;
        this.policyDecision = policyDecision;
        this.externalResourceType = externalResourceType;
        this.externalResourceId = externalResourceId;
        this.amountMinor = amountMinor;
        this.createdAt = createdAt;
        this.approvedAt = approvedAt;
        this.executedAt = executedAt;
    }

    public UUID getId() { return id; }
    public UUID getRecoveryPlanId() { return recoveryPlan.getId(); }
    public UUID getIncidentId() { return incident.getIncidentId(); }
    public RecoveryActionStatus getStatus() { return status; }
    public PolicyDecision getPolicyDecision() { return policyDecision; }
    public String getExternalResourceType() { return externalResourceType; }
    public String getExternalResourceId() { return externalResourceId; }
    public long getAmountMinor() { return amountMinor; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getExecutedAt() { return executedAt; }
}
