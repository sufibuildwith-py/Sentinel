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

    @Column(name = "provider_link_id", length = 128)
    private String providerLinkId;

    @Column(name = "payload_digest", length = 64)
    private String payloadDigest;

    @Column(nullable = false, length = 32)
    private String disposition = "RECEIVED";

    @Column(name = "processing_error", length = 64)
    private String processingError;

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
    public String getProviderLinkId() { return providerLinkId; }
    public String getPayloadDigest() { return payloadDigest; }
    public String getDisposition() { return disposition; }
    public String getProcessingError() { return processingError; }

    public static ProcessedWebhookEvent received(String eventId, String digest, Instant receivedAt) {
        ProcessedWebhookEvent event = new ProcessedWebhookEvent(eventId, "PENDING", receivedAt,
                null, Map.of(), true);
        event.payloadDigest = digest;
        return event;
    }

    public void complete(String eventType, String providerLinkId, Map<String, Object> minimizedPayload,
                         String disposition, String processingError, Instant processedAt) {
        this.eventType = eventType;
        this.providerLinkId = providerLinkId;
        this.rawPayload = minimizedPayload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(minimizedPayload);
        this.disposition = disposition;
        this.processingError = processingError;
        this.processedAt = processedAt;
    }
}
