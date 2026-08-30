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
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true, length = 255)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false, columnDefinition = "text")
    private String signature;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private boolean processed;

    @Column(name = "incident_id")
    private UUID incidentId;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected WebhookEvent() {
    }

    public WebhookEvent(String eventId, String eventType, String payload, String signature) {
        this(eventId, eventType, payload, signature, false, false, null, Instant.now(), null);
    }

    public WebhookEvent(String eventId, String eventType, String payload,
                        String signature, boolean verified, boolean processed,
                        UUID incidentId, Instant receivedAt, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.signature = signature;
        this.verified = verified;
        this.processed = processed;
        this.incidentId = incidentId;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
    }

    public UUID getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public String getSignature() { return signature; }
    public boolean isVerified() { return verified; }
    public boolean isProcessed() { return processed; }
    public UUID getIncidentId() { return incidentId; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getProcessedAt() { return processedAt; }

    public void markProcessed(UUID incidentId, Instant processedAt) {
        this.processed = true;
        this.incidentId = incidentId;
        this.processedAt = processedAt;
    }

    public void associateIncident(UUID incidentId) {
        this.incidentId = incidentId;
    }
}
