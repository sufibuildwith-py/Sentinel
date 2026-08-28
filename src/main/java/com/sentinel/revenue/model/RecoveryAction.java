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
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;
import com.sentinel.revenue.policy.PolicyEvaluation;

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

    @Column(name = "target_payment_id", length = 128)
    private String targetPaymentId;

    @Column(name = "target_customer_id", length = 128)
    private String targetCustomerId;

    @Column(length = 3)
    private String currency;

    @Column(name = "provider_reference_id", length = 40)
    private String providerReferenceId;

    @Column(name = "external_resource_url", columnDefinition = "text")
    private String externalResourceUrl;

    @Column(name = "external_resource_status", length = 32)
    private String externalResourceStatus;

    @Column(name = "execution_attempts", nullable = false)
    private int executionAttempts;

    @Column(name = "execution_claimed_at")
    private Instant executionClaimedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_error_code", length = 64)
    private String lastErrorCode;

    @Version
    private long version;

    protected RecoveryAction() {
    }

    private RecoveryAction(RecoveryPlan recoveryPlan, RevenueIncident incident,
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

    public static RecoveryAction fromPersistedPolicy(RecoveryPlan plan, RevenueIncident incident,
                                                     PolicyEvaluation policy, long amountMinor,
                                                     Instant createdAt) {
        if (policy == null || policy.rules().isEmpty()) {
            throw new IllegalArgumentException("A traced policy result is required before action creation");
        }
        RecoveryActionStatus status = switch (policy.decision()) {
            case AUTO -> RecoveryActionStatus.AUTO_APPROVED;
            case HUMAN -> RecoveryActionStatus.PENDING_APPROVAL;
            case DENY -> RecoveryActionStatus.STOPPED;
        };
        return new RecoveryAction(plan, incident, status, policy.decision(), null, null,
                amountMinor, createdAt, null, null);
    }

    public void approve(Instant approvedAt) {
        if (status != RecoveryActionStatus.PENDING_APPROVAL || policyDecision != PolicyDecision.HUMAN) {
            throw new IllegalStateException("Only a HUMAN-gated pending action can be approved");
        }
        status = RecoveryActionStatus.APPROVED;
        this.approvedAt = approvedAt;
    }

    public void reject() {
        if (status != RecoveryActionStatus.PENDING_APPROVAL || policyDecision != PolicyDecision.HUMAN) {
            throw new IllegalStateException("Only a HUMAN-gated pending action can be rejected");
        }
        status = RecoveryActionStatus.REJECTED;
    }

    public void claim(String paymentId, String customerId, String currency, long exactAmountMinor,
                      String referenceId, Instant claimedAt, Instant expiresAt) {
        if (status != RecoveryActionStatus.AUTO_APPROVED && status != RecoveryActionStatus.APPROVED
                && status != RecoveryActionStatus.RETRY_PENDING
                && status != RecoveryActionStatus.EXECUTION_UNCERTAIN) {
            throw new IllegalStateException("Action is not eligible for execution");
        }
        if (policyDecision == PolicyDecision.HUMAN && approvedAt == null) {
            throw new IllegalStateException("HUMAN policy action has no recorded approval");
        }
        this.targetPaymentId = paymentId;
        this.targetCustomerId = customerId;
        this.currency = currency;
        this.amountMinor = exactAmountMinor;
        this.providerReferenceId = referenceId;
        this.executionClaimedAt = claimedAt;
        this.expiresAt = expiresAt;
        this.executionAttempts++;
        this.status = RecoveryActionStatus.EXECUTING;
        this.lastErrorCode = null;
    }

    public void complete(String resourceId, String shortUrl, String providerStatus, Instant completedAt) {
        requireExecuting();
        this.externalResourceType = "payment_link";
        this.externalResourceId = resourceId;
        this.externalResourceUrl = shortUrl;
        this.externalResourceStatus = providerStatus;
        this.executedAt = completedAt;
        this.status = RecoveryActionStatus.EXECUTED;
    }

    public void retryPending(String safeErrorCode, boolean uncertain) {
        requireExecuting();
        this.lastErrorCode = safeErrorCode;
        this.status = uncertain ? RecoveryActionStatus.EXECUTION_UNCERTAIN
                : RecoveryActionStatus.RETRY_PENDING;
    }

    public void executionFailed(String safeErrorCode) {
        requireExecuting();
        this.lastErrorCode = safeErrorCode;
        this.status = RecoveryActionStatus.FAILED;
    }

    public void stop(String safeReason) {
        this.lastErrorCode = safeReason;
        this.status = RecoveryActionStatus.STOPPED;
    }

    public void recordPartial(String providerStatus) {
        if (status == RecoveryActionStatus.RECOVERED || status == RecoveryActionStatus.CANCELLED) return;
        if (externalResourceId == null) throw new IllegalStateException("Action has no Payment Link");
        this.externalResourceStatus = providerStatus;
        this.status = RecoveryActionStatus.PARTIALLY_RECOVERED;
    }

    public void recordRecovered(String providerStatus) {
        if (externalResourceId == null) throw new IllegalStateException("Action has no Payment Link");
        this.externalResourceStatus = providerStatus;
        this.status = RecoveryActionStatus.RECOVERED;
    }

    public void recordCancelled(String providerStatus) {
        if (status == RecoveryActionStatus.RECOVERED) return;
        if (externalResourceId == null) throw new IllegalStateException("Action has no Payment Link");
        this.externalResourceStatus = providerStatus;
        this.status = RecoveryActionStatus.CANCELLED;
    }

    private void requireExecuting() {
        if (status != RecoveryActionStatus.EXECUTING) {
            throw new IllegalStateException("Action has not been claimed for execution");
        }
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
    public String getTargetPaymentId() { return targetPaymentId; }
    public String getTargetCustomerId() { return targetCustomerId; }
    public String getCurrency() { return currency; }
    public String getProviderReferenceId() { return providerReferenceId; }
    public String getExternalResourceUrl() { return externalResourceUrl; }
    public String getExternalResourceStatus() { return externalResourceStatus; }
    public int getExecutionAttempts() { return executionAttempts; }
    public Instant getExecutionClaimedAt() { return executionClaimedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getLastErrorCode() { return lastErrorCode; }
    public long getVersion() { return version; }
}
