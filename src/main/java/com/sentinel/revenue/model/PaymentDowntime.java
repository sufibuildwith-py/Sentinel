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
@Table(name = "payment_downtimes")
public class PaymentDowntime {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "razorpay_id", length = 255)
    private String razorpayId;

    @Column(length = 100)
    private String method;

    @Column(length = 100)
    private String instrument;

    @Column(name = "begin_at")
    private Instant beginAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(length = 50)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "fetched_at", nullable = false, updatable = false)
    private Instant fetchedAt;

    protected PaymentDowntime() {
    }

    public PaymentDowntime(String razorpayId, String method, String instrument,
                           Instant beginAt, Instant endAt, String status,
                           String rawPayload) {
        this.razorpayId = razorpayId;
        this.method = method;
        this.instrument = instrument;
        this.beginAt = beginAt;
        this.endAt = endAt;
        this.status = status;
        this.rawPayload = rawPayload;
    }

    public UUID getId() { return id; }
    public String getRazorpayId() { return razorpayId; }
    public String getMethod() { return method; }
    public String getInstrument() { return instrument; }
    public Instant getBeginAt() { return beginAt; }
    public Instant getEndAt() { return endAt; }
    public String getStatus() { return status; }
    public String getRawPayload() { return rawPayload; }
    public Instant getFetchedAt() { return fetchedAt; }
}
