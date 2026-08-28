package com.sentinel.revenue.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_security_events")
public class WebhookSecurityEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "received_at", nullable = false) private Instant receivedAt;
    @Column(name = "request_digest", nullable = false, length = 64) private String requestDigest;
    @Column(name = "signature_header_present", nullable = false) private boolean signatureHeaderPresent;
    @Column(name = "event_id_header_present", nullable = false) private boolean eventIdHeaderPresent;
    @Column(nullable = false, length = 64) private String reason;
    protected WebhookSecurityEvent() { }
    public WebhookSecurityEvent(Instant receivedAt, String requestDigest, boolean signatureHeaderPresent,
                                boolean eventIdHeaderPresent, String reason) {
        this.receivedAt = receivedAt; this.requestDigest = requestDigest;
        this.signatureHeaderPresent = signatureHeaderPresent;
        this.eventIdHeaderPresent = eventIdHeaderPresent; this.reason = reason;
    }
    public UUID getId() { return id; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getRequestDigest() { return requestDigest; }
    public boolean isSignatureHeaderPresent() { return signatureHeaderPresent; }
    public boolean isEventIdHeaderPresent() { return eventIdHeaderPresent; }
    public String getReason() { return reason; }
}
