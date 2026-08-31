package com.sentinel.revenue.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "systemic_incident_members")
public class SystemicIncidentMember {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "systemic_incident_id", nullable = false)
    private SystemicRecoveryIncident systemicIncident;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_incident_id", nullable = false)
    private RevenueIncident paymentIncident;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SystemicIncidentMember() { }
    public SystemicIncidentMember(SystemicRecoveryIncident parent, RevenueIncident child, Instant createdAt) {
        this.systemicIncident = parent; this.paymentIncident = child; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getSystemicIncidentId() { return systemicIncident.getId(); }
    public UUID getPaymentIncidentId() { return paymentIncident.getIncidentId(); }
    public Instant getCreatedAt() { return createdAt; }
}
