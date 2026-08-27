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
@Table(name = "recovery_outcomes")
public class RecoveryOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recovery_action_id", nullable = false)
    private RecoveryAction recoveryAction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private RevenueIncident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecoveryOutcomeStatus status;

    @Column(name = "recovered_amount_minor", nullable = false)
    private long recoveredAmountMinor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "source_event_id", length = 128)
    private String sourceEventId;

    protected RecoveryOutcome() {
    }

    public RecoveryOutcome(RecoveryAction recoveryAction, RevenueIncident incident,
                           RecoveryOutcomeStatus status, long recoveredAmountMinor,
                           Instant occurredAt, String sourceEventId) {
        this.recoveryAction = recoveryAction;
        this.incident = incident;
        this.status = status;
        this.recoveredAmountMinor = recoveredAmountMinor;
        this.occurredAt = occurredAt;
        this.sourceEventId = sourceEventId;
    }

    public UUID getId() { return id; }
    public UUID getRecoveryActionId() { return recoveryAction.getId(); }
    public UUID getIncidentId() { return incident.getIncidentId(); }
    public RecoveryOutcomeStatus getStatus() { return status; }
    public long getRecoveredAmountMinor() { return recoveredAmountMinor; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getSourceEventId() { return sourceEventId; }
}
