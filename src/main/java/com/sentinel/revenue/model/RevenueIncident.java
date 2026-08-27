package com.sentinel.revenue.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "revenue_incidents")
public class RevenueIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(nullable = false, length = 128)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RevenueIncidentStatus status;

    @Column(nullable = false, length = 32)
    private String severity;

    @Column(name = "amount_at_risk_minor", nullable = false)
    private long amountAtRiskMinor;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_payments", nullable = false, columnDefinition = "jsonb")
    private List<String> affectedPayments = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_customers", nullable = false, columnDefinition = "jsonb")
    private List<String> affectedCustomers = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> evidence = new ArrayList<>();

    @Column(name = "root_cause", columnDefinition = "text")
    private String rootCause;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_decision", length = 16)
    private PolicyDecision policyDecision;

    protected RevenueIncident() {
    }

    public RevenueIncident(String type, RevenueIncidentStatus status, String severity,
                           long amountAtRiskMinor, Instant detectedAt,
                           List<String> affectedPayments, List<String> affectedCustomers,
                           List<String> evidence, String rootCause,
                           PolicyDecision policyDecision) {
        this.type = type;
        this.status = status;
        this.severity = severity;
        this.amountAtRiskMinor = amountAtRiskMinor;
        this.detectedAt = detectedAt;
        this.affectedPayments = affectedPayments == null ? new ArrayList<>() : new ArrayList<>(affectedPayments);
        this.affectedCustomers = affectedCustomers == null ? new ArrayList<>() : new ArrayList<>(affectedCustomers);
        this.evidence = evidence == null ? new ArrayList<>() : new ArrayList<>(evidence);
        this.rootCause = rootCause;
        this.policyDecision = policyDecision;
    }

    public UUID getIncidentId() { return incidentId; }
    public String getType() { return type; }
    public RevenueIncidentStatus getStatus() { return status; }
    public String getSeverity() { return severity; }
    public long getAmountAtRiskMinor() { return amountAtRiskMinor; }
    public Instant getDetectedAt() { return detectedAt; }
    public List<String> getAffectedPayments() { return List.copyOf(affectedPayments); }
    public List<String> getAffectedCustomers() { return List.copyOf(affectedCustomers); }
    public List<String> getEvidence() { return List.copyOf(evidence); }
    public String getRootCause() { return rootCause; }
    public PolicyDecision getPolicyDecision() { return policyDecision; }

    public void transitionTo(RevenueIncidentStatus newStatus) {
        this.status = newStatus;
    }
}
