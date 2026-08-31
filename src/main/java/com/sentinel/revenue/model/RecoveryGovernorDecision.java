package com.sentinel.revenue.model;

import com.sentinel.revenue.governor.ExecutionEnvelope;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "recovery_governor_decisions")
public class RecoveryGovernorDecision {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;
    @Column(name = "recovery_action_id", nullable = false)
    private UUID recoveryActionId;
    @Column(nullable = false)
    private boolean allowed;
    @Column(name = "allowed_value_minor", nullable = false)
    private long allowedValueMinor;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb")
    private ExecutionEnvelope envelope;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> violations = new ArrayList<>();
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    protected RecoveryGovernorDecision() { }
    public RecoveryGovernorDecision(UUID incidentId, UUID actionId, boolean allowed,
                                    long allowedValueMinor, ExecutionEnvelope envelope,
                                    List<String> violations, Instant createdAt) {
        this.incidentId = incidentId; this.recoveryActionId = actionId; this.allowed = allowed;
        this.allowedValueMinor = allowedValueMinor; this.envelope = envelope;
        this.violations = new ArrayList<>(violations); this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public UUID getRecoveryActionId() { return recoveryActionId; }
    public boolean isAllowed() { return allowed; }
    public long getAllowedValueMinor() { return allowedValueMinor; }
    public ExecutionEnvelope getEnvelope() { return envelope; }
    public List<String> getViolations() { return List.copyOf(violations); }
    public Instant getCreatedAt() { return createdAt; }
}
