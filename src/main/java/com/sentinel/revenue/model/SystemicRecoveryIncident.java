package com.sentinel.revenue.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "systemic_recovery_incidents")
public class SystemicRecoveryIncident {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "merchant_id", nullable = false, length = 128)
    private String merchantId;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(nullable = false, length = 128)
    private String scope;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "root_cause_candidates", nullable = false, columnDefinition = "jsonb")
    private List<RootCauseCandidate> rootCauseCandidates = new ArrayList<>();
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SystemicRecoveryIncident() { }
    public SystemicRecoveryIncident(String merchantId, String scope,
                                    List<RootCauseCandidate> candidates, Instant now) {
        this.merchantId = merchantId;
        this.scope = scope;
        this.status = "OPEN";
        this.rootCauseCandidates = new ArrayList<>(candidates);
        this.createdAt = now;
        this.updatedAt = now;
    }
    public UUID getId() { return id; }
    public String getMerchantId() { return merchantId; }
    public String getStatus() { return status; }
    public String getScope() { return scope; }
    public List<RootCauseCandidate> getRootCauseCandidates() { return List.copyOf(rootCauseCandidates); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
