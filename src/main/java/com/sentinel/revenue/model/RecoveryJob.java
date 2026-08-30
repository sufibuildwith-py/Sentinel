package com.sentinel.revenue.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recovery_jobs")
public class RecoveryJob {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";
    public static final String EXHAUSTED = "EXHAUSTED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "policy_decision_id")
    private UUID policyDecisionId;

    @Column(nullable = false, length = 50)
    private String status = PENDING;

    @Column(length = 100)
    private String strategy;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_detail", columnDefinition = "text")
    private String errorDetail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RecoveryJob() {
    }

    public RecoveryJob(UUID incidentId, UUID policyDecisionId, String strategy) {
        this(incidentId, policyDecisionId, strategy, 3, Instant.now());
    }

    public RecoveryJob(UUID incidentId, UUID policyDecisionId, String strategy,
                       int maxAttempts, Instant nextAttemptAt) {
        this.incidentId = incidentId;
        this.policyDecisionId = policyDecisionId;
        this.strategy = strategy;
        this.maxAttempts = maxAttempts;
        this.nextAttemptAt = nextAttemptAt;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public UUID getPolicyDecisionId() { return policyDecisionId; }
    public String getStatus() { return status; }
    public String getStrategy() { return strategy; }
    public int getAttemptCount() { return attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }
    public Instant getLastAttemptedAt() { return lastAttemptedAt; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getErrorDetail() { return errorDetail; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void markRunning(Instant now) {
        requireStatus(PENDING);
        status = RUNNING;
        attemptCount++;
        lastAttemptedAt = now;
        nextAttemptAt = null;
        errorDetail = null;
    }

    public void markSucceeded(Instant now) {
        requireStatus(RUNNING);
        status = SUCCEEDED;
        completedAt = now;
    }

    public void markFailed(String errorDetail, Instant now) {
        requireStatus(RUNNING);
        this.errorDetail = errorDetail;
        if (attemptCount >= maxAttempts) {
            status = EXHAUSTED;
            completedAt = now;
            nextAttemptAt = null;
        } else {
            status = PENDING;
            nextAttemptAt = now.plusSeconds(5 * 60L);
        }
    }

    public void markExhausted(Instant now) {
        if (!PENDING.equals(status) && !RUNNING.equals(status)) {
            throw new IllegalStateException("Recovery job cannot be exhausted from " + status);
        }
        status = EXHAUSTED;
        completedAt = now;
        nextAttemptAt = null;
    }

    private void requireStatus(String expected) {
        if (!expected.equals(status)) {
            throw new IllegalStateException("Recovery job must be " + expected + " but was " + status);
        }
    }
}
