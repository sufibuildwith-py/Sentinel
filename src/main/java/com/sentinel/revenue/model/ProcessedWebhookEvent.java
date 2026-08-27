package com.sentinel.revenue.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "processed_webhooks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_processed_webhooks_event_id",
                columnNames = "event_id"))
public class ProcessedWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawPayload = new LinkedHashMap<>();

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid;

    protected ProcessedWebhookEvent() {
    }

    public ProcessedWebhookEvent(String eventId, String eventType, Instant receivedAt,
                                 Instant processedAt, Map<String, Object> rawPayload,
                                 boolean signatureValid) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
        this.rawPayload = rawPayload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rawPayload);
        this.signatureValid = signatureValid;
    }

    public UUID getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public Map<String, Object> getRawPayload() { return Map.copyOf(rawPayload); }
    public boolean isSignatureValid() { return signatureValid; }
}
