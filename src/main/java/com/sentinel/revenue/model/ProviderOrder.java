package com.sentinel.revenue.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_orders")
public class ProviderOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "razorpay_order_id", nullable = false, unique = true, length = 255)
    private String razorpayOrderId;

    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(nullable = false, length = 50)
    private String status = "CREATED";

    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 32)
    private ExecutionMode executionMode = ExecutionMode.LEGACY_UNSPECIFIED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProviderOrder() {
    }

    public ProviderOrder(UUID incidentId, String razorpayOrderId, long amountPaise,
                         String currency, String status, String providerReference,
                         String idempotencyKey) {
        this(incidentId, razorpayOrderId, amountPaise, currency, status, providerReference,
                idempotencyKey, ExecutionMode.LEGACY_UNSPECIFIED);
    }

    public ProviderOrder(UUID incidentId, String razorpayOrderId, long amountPaise,
                         String currency, String status, String providerReference,
                         String idempotencyKey, ExecutionMode executionMode) {
        this.incidentId = incidentId;
        this.razorpayOrderId = razorpayOrderId;
        this.amountPaise = amountPaise;
        this.currency = currency == null ? "INR" : currency;
        this.status = status == null ? "CREATED" : status;
        this.providerReference = providerReference;
        this.idempotencyKey = idempotencyKey;
        this.executionMode = executionMode == null ? ExecutionMode.LEGACY_UNSPECIFIED : executionMode;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public long getAmountPaise() { return amountPaise; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getProviderReference() { return providerReference; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public ExecutionMode getExecutionMode() { return executionMode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateProviderState(String status, String providerReference) {
        this.status = status;
        this.providerReference = providerReference;
    }
}
