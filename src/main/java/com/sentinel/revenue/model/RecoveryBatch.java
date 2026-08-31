package com.sentinel.revenue.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "recovery_batches")
public class RecoveryBatch {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 64) private String strategy;
    @Column(nullable = false, length = 32) private String status;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "incident_ids", nullable = false, columnDefinition = "jsonb")
    private List<UUID> incidentIds = new ArrayList<>();
    @Column(name = "canary_size", nullable = false) private int canarySize;
    @Column(name = "released_count", nullable = false) private int releasedCount;
    @Column(name = "required_reconciled_count", nullable = false) private int requiredReconciledCount;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected RecoveryBatch() { }
    public RecoveryBatch(String strategy, List<UUID> incidentIds, int canarySize,
                         int requiredReconciledCount, Instant now) {
        this.strategy = strategy; this.incidentIds = new ArrayList<>(incidentIds);
        this.canarySize = canarySize; this.requiredReconciledCount = requiredReconciledCount;
        this.releasedCount = Math.min(canarySize, incidentIds.size());
        this.status = this.releasedCount == incidentIds.size() ? "FULLY_RELEASED" : "CANARY_RELEASED";
        this.createdAt = now; this.updatedAt = now;
    }
    public void expand(int count, Instant now) {
        if (!"CANARY_RELEASED".equals(status)) throw new IllegalStateException("Batch is not awaiting canary reconciliation");
        releasedCount = Math.min(incidentIds.size(), releasedCount + count);
        status = releasedCount == incidentIds.size() ? "FULLY_RELEASED" : "CANARY_RELEASED";
        updatedAt = now;
    }
    public UUID getId() { return id; }
    public String getStrategy() { return strategy; }
    public String getStatus() { return status; }
    public List<UUID> getIncidentIds() { return List.copyOf(incidentIds); }
    public int getCanarySize() { return canarySize; }
    public int getReleasedCount() { return releasedCount; }
    public int getRequiredReconciledCount() { return requiredReconciledCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
