package com.sentinel.revenue.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payment_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_events_payment_attempt",
                columnNames = {"payment_id", "attempt_number"}),
        indexes = {
                @Index(name = "idx_payment_events_timestamp", columnList = "event_timestamp"),
                @Index(name = "idx_payment_events_customer", columnList = "customer_id")
        })
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false, length = 128)
    private String paymentId;

    @Column(name = "order_id", nullable = false, length = 128)
    private String orderId;

    @Column(name = "customer_id", nullable = false, length = 128)
    private String customerId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 64)
    private String method;

    @Column(name = "issuer_bank", length = 128)
    private String issuerBank;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    @Column(name = "error_description", columnDefinition = "text")
    private String errorDescription;

    @Column(name = "event_timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "previous_successful_method", length = 64)
    private String previousSuccessfulMethod;

    @Column(name = "previous_failure_count", nullable = false)
    private int previousFailureCount;

    @Column(name = "subscription_id", length = 128)
    private String subscriptionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    protected PaymentEvent() {
    }

    public PaymentEvent(String paymentId, String orderId, String customerId, long amountMinor,
                        String currency, String method, String issuerBank, String status,
                        String errorCode, String errorDescription, Instant timestamp,
                        int attemptNumber, String previousSuccessfulMethod,
                        int previousFailureCount, String subscriptionId,
                        Map<String, Object> metadata) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.method = method;
        this.issuerBank = issuerBank;
        this.status = status;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.timestamp = timestamp;
        this.attemptNumber = attemptNumber;
        this.previousSuccessfulMethod = previousSuccessfulMethod;
        this.previousFailureCount = previousFailureCount;
        this.subscriptionId = subscriptionId;
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public UUID getId() { return id; }
    public String getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public String getMethod() { return method; }
    public String getIssuerBank() { return issuerBank; }
    public String getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public String getErrorDescription() { return errorDescription; }
    public Instant getTimestamp() { return timestamp; }
    public int getAttemptNumber() { return attemptNumber; }
    public String getPreviousSuccessfulMethod() { return previousSuccessfulMethod; }
    public int getPreviousFailureCount() { return previousFailureCount; }
    public String getSubscriptionId() { return subscriptionId; }
    public Map<String, Object> getMetadata() { return Map.copyOf(metadata); }
}
