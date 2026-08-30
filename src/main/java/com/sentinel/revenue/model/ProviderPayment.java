package com.sentinel.revenue.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_payments")
public class ProviderPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "provider_order_id")
    private UUID providerOrderId;

    @Column(name = "razorpay_payment_id", nullable = false, unique = true, length = 255)
    private String razorpayPaymentId;

    @Column(name = "razorpay_order_id", nullable = false, length = 255)
    private String razorpayOrderId;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "amount_paise")
    private Long amountPaise;

    @Column(length = 100)
    private String method;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProviderPayment() {
    }

    public ProviderPayment(UUID providerOrderId, String razorpayPaymentId,
                           String razorpayOrderId, String status, Long amountPaise,
                           String method, Instant capturedAt, String rawPayload) {
        this.providerOrderId = providerOrderId;
        this.razorpayPaymentId = razorpayPaymentId;
        this.razorpayOrderId = razorpayOrderId;
        this.status = status;
        this.amountPaise = amountPaise;
        this.method = method;
        this.capturedAt = capturedAt;
        this.rawPayload = rawPayload;
    }

    public UUID getId() { return id; }
    public UUID getProviderOrderId() { return providerOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public String getStatus() { return status; }
    public Long getAmountPaise() { return amountPaise; }
    public String getMethod() { return method; }
    public Instant getCapturedAt() { return capturedAt; }
    public String getRawPayload() { return rawPayload; }
    public Instant getCreatedAt() { return createdAt; }
}
