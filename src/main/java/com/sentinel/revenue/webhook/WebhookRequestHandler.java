package com.sentinel.revenue.webhook;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class WebhookRequestHandler {
    private final WebhookSignatureVerifier verifier;
    private final WebhookSecurityAuditService securityAudit;
    private final WebhookOutcomeProcessor processor;
    public WebhookRequestHandler(WebhookSignatureVerifier verifier, WebhookSecurityAuditService securityAudit,
                                 WebhookOutcomeProcessor processor) {
        this.verifier = verifier; this.securityAudit = securityAudit; this.processor = processor;
    }

    public WebhookResult handle(byte[] rawBody, String signature, String eventId) {
        byte[] body = rawBody == null ? new byte[0] : rawBody;
        String digest = verifier.digest(body);
        boolean signaturePresent = signature != null && !signature.isBlank();
        boolean eventIdPresent = eventId != null && !eventId.isBlank();
        if (!verifier.verify(body, signature)) {
            securityAudit.record(digest, signaturePresent, eventIdPresent, "INVALID_SIGNATURE");
            throw new InvalidWebhookSignatureException();
        }
        if (!eventIdPresent) {
            securityAudit.record(digest, true, false, "MISSING_EVENT_ID");
            throw new IllegalArgumentException("X-Razorpay-Event-Id is required");
        }
        if (processor.alreadyProcessed(eventId)) return processor.duplicate(eventId);
        try { return processor.process(eventId, body, digest); }
        catch (DataIntegrityViolationException concurrentDuplicate) { return processor.duplicate(eventId); }
    }
}
