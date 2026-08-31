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

    @Column(name = "provider_confirmed", nullable = false)
    private boolean providerConfirmed;

    @Column(name = "confirmation_source", length = 32)
    private String confirmationSource;

    @Column(name = "feature_schema_version", nullable = false, length = 32)
    private String featureSchemaVersion = "recovery-v1";

    @Column(name = "model_version", nullable = false, length = 64)
    private String modelVersion = "none-deterministic";

    @Column(name = "policy_version", nullable = false, length = 64)
    private String policyVersion = "policy-v1";

    @Column(name = "strategy_version", nullable = false, length = 64)
    private String strategyVersion = "strategy-v1";

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
        this.providerConfirmed = sourceEventId != null && !sourceEventId.isBlank()
                && !sourceEventId.startsWith("provider-order:");
        this.confirmationSource = providerConfirmed ? "LEGACY_RECONCILED" : null;
    }

    public static RecoveryOutcome providerConfirmed(RecoveryAction recoveryAction,
                                                     RevenueIncident incident,
                                                     RecoveryOutcomeStatus status,
                                                     long recoveredAmountMinor,
                                                     Instant occurredAt,
                                                     String sourceEventId,
                                                     String confirmationSource) {
        if (sourceEventId == null || sourceEventId.isBlank()) {
            throw new IllegalArgumentException("Provider confirmation requires a source event");
        }
        if (confirmationSource == null || confirmationSource.isBlank()) {
            throw new IllegalArgumentException("Provider confirmation source is required");
        }
        RecoveryOutcome outcome = new RecoveryOutcome(recoveryAction, incident, status,
                recoveredAmountMinor, occurredAt, sourceEventId);
        outcome.providerConfirmed = true;
        outcome.confirmationSource = confirmationSource;
        return outcome;
    }

    public UUID getId() { return id; }
    public UUID getRecoveryActionId() { return recoveryAction.getId(); }
    public UUID getIncidentId() { return incident.getIncidentId(); }
    public RecoveryOutcomeStatus getStatus() { return status; }
    public long getRecoveredAmountMinor() { return recoveredAmountMinor; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getSourceEventId() { return sourceEventId; }
    public boolean isProviderConfirmed() { return providerConfirmed; }
    public String getConfirmationSource() { return confirmationSource; }
    public String getFeatureSchemaVersion() { return featureSchemaVersion; }
    public String getModelVersion() { return modelVersion; }
    public String getPolicyVersion() { return policyVersion; }
    public String getStrategyVersion() { return strategyVersion; }

    public boolean isTerminalPaid() { return status == RecoveryOutcomeStatus.RECOVERED; }

    public boolean applyPartial(long cumulativeAmountMinor, Instant at, String eventId) {
        if (isTerminalPaid() || status == RecoveryOutcomeStatus.STOPPED
                || cumulativeAmountMinor <= recoveredAmountMinor) return false;
        this.status = RecoveryOutcomeStatus.PARTIALLY_RECOVERED;
        this.recoveredAmountMinor = cumulativeAmountMinor;
        this.occurredAt = at;
        this.sourceEventId = eventId;
        confirmFromEvent(eventId);
        return true;
    }

    public boolean applyRecovered(long cumulativeAmountMinor, Instant at, String eventId) {
        if (isTerminalPaid()) return false;
        this.status = RecoveryOutcomeStatus.RECOVERED;
        this.recoveredAmountMinor = cumulativeAmountMinor;
        this.occurredAt = at;
        this.sourceEventId = eventId;
        confirmFromEvent(eventId);
        return true;
    }

    public boolean applyCancelled(Instant at, String eventId) {
        if (isTerminalPaid()) return false;
        this.status = RecoveryOutcomeStatus.STOPPED;
        this.occurredAt = at;
        this.sourceEventId = eventId;
        confirmFromEvent(eventId);
        return true;
    }

    private void confirmFromEvent(String eventId) {
        if (eventId != null && !eventId.isBlank() && !eventId.startsWith("provider-order:")) {
            providerConfirmed = true;
            if (confirmationSource == null) confirmationSource = "RECONCILED_EVENT";
        }
    }
}
