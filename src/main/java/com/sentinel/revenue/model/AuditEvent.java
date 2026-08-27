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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id")
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private RevenueIncident incident;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 128)
    private String actor;

    @Column(length = 128)
    private String agent;

    @Column(nullable = false, length = 128)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_evidence", nullable = false, columnDefinition = "jsonb")
    private List<String> inputEvidence = new ArrayList<>();

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(columnDefinition = "text")
    private String decision;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy_rules_evaluated", nullable = false, columnDefinition = "jsonb")
    private List<String> policyRulesEvaluated = new ArrayList<>();

    @Column(name = "policy_result", length = 64)
    private String policyResult;

    @Column(name = "external_resource_id", length = 128)
    private String externalResourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_state", length = 32)
    private RevenueIncidentStatus previousState;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_state", length = 32)
    private RevenueIncidentStatus newState;

    @Column(columnDefinition = "text")
    private String outcome;

    protected AuditEvent() {
    }

    public AuditEvent(RevenueIncident incident, Instant timestamp, String actor, String agent,
                      String action, List<String> inputEvidence, BigDecimal confidence,
                      String decision, List<String> policyRulesEvaluated, String policyResult,
                      String externalResourceId, RevenueIncidentStatus previousState,
                      RevenueIncidentStatus newState, String outcome) {
        this.incident = incident;
        this.timestamp = timestamp;
        this.actor = actor;
        this.agent = agent;
        this.action = action;
        this.inputEvidence = inputEvidence == null ? new ArrayList<>() : new ArrayList<>(inputEvidence);
        this.confidence = confidence;
        this.decision = decision;
        this.policyRulesEvaluated = policyRulesEvaluated == null
                ? new ArrayList<>() : new ArrayList<>(policyRulesEvaluated);
        this.policyResult = policyResult;
        this.externalResourceId = externalResourceId;
        this.previousState = previousState;
        this.newState = newState;
        this.outcome = outcome;
    }

    public UUID getEventId() { return eventId; }
    public UUID getIncidentId() { return incident.getIncidentId(); }
    public Instant getTimestamp() { return timestamp; }
    public String getActor() { return actor; }
    public String getAgent() { return agent; }
    public String getAction() { return action; }
    public List<String> getInputEvidence() { return List.copyOf(inputEvidence); }
    public BigDecimal getConfidence() { return confidence; }
    public String getDecision() { return decision; }
    public List<String> getPolicyRulesEvaluated() { return List.copyOf(policyRulesEvaluated); }
    public String getPolicyResult() { return policyResult; }
    public String getExternalResourceId() { return externalResourceId; }
    public RevenueIncidentStatus getPreviousState() { return previousState; }
    public RevenueIncidentStatus getNewState() { return newState; }
    public String getOutcome() { return outcome; }
}
