package com.sentinel.revenue.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.core.observability.RequestContext;
import com.sentinel.revenue.model.WebhookEvent;
import com.sentinel.revenue.service.WebhookEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class WebhookRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WebhookRequestHandler.class);
    private final WebhookSignatureVerifier verifier;
    private final WebhookSecurityAuditService securityAudit;
    private final WebhookOutcomeProcessor processor;
    private final ObjectMapper json;
    private final WebhookEventService eventService;
    private final RecoveryJobEnqueuer jobEnqueuer;

    public WebhookRequestHandler(WebhookSignatureVerifier verifier, WebhookSecurityAuditService securityAudit,
                                 WebhookOutcomeProcessor processor) {
        this(verifier, securityAudit, processor, new ObjectMapper(),
                (WebhookEventService) null, (RecoveryJobEnqueuer) null);
    }

    @Autowired
    public WebhookRequestHandler(WebhookSignatureVerifier verifier,
                                 WebhookSecurityAuditService securityAudit,
                                 WebhookOutcomeProcessor processor,
                                 ObjectMapper json,
                                 ObjectProvider<WebhookEventService> eventServices,
                                 ObjectProvider<RecoveryJobEnqueuer> jobEnqueuers) {
        this(verifier, securityAudit, processor, json,
                eventServices.getIfAvailable(), jobEnqueuers.getIfAvailable());
    }

    WebhookRequestHandler(WebhookSignatureVerifier verifier,
                          WebhookSecurityAuditService securityAudit,
                          WebhookOutcomeProcessor processor,
                          ObjectMapper json,
                          WebhookEventService eventService,
                          RecoveryJobEnqueuer jobEnqueuer) {
        this.verifier = verifier;
        this.securityAudit = securityAudit;
        this.processor = processor;
        this.json = json;
        this.eventService = eventService;
        this.jobEnqueuer = jobEnqueuer;
    }

    public WebhookResult handle(byte[] rawBody, String signature, String eventId) {
        byte[] body = rawBody == null ? new byte[0] : rawBody;
        String digest = verifier.digest(body);
        boolean signaturePresent = signature != null && !signature.isBlank();
        boolean eventIdPresent = eventId != null && !eventId.isBlank();
        if (!verifier.verify(body, signature)) {
            securityAudit.record(digest, signaturePresent, eventIdPresent, "INVALID_SIGNATURE");
            log.warn("[{}] Razorpay webhook signature validation failed",
                    requestId());
            throw new InvalidWebhookSignatureBadRequestException();
        }
        if (!eventIdPresent) {
            securityAudit.record(digest, true, false, "MISSING_EVENT_ID");
            throw new IllegalArgumentException("X-Razorpay-Event-Id is required");
        }
        if (isDuplicate(eventId)) {
            log.debug("duplicate webhook event_id={} ignored", eventId);
            return processor.duplicate(eventId);
        }
        JsonNode payload;
        try {
            payload = json.readTree(body);
        } catch (IOException malformed) {
            throw new IllegalArgumentException("Webhook payload is invalid", malformed);
        }
        try {
            String eventType = payload.path("event").asText();
            if (eventType.isBlank()) {
                throw new IllegalArgumentException("Webhook event type is required");
            }

            WebhookEvent persisted = persist(eventId, eventType, body, signature);
            if (persisted != null && jobEnqueuer != null
                    && ("payment.failed".equals(eventType) || "payment.captured".equals(eventType))) {
                boolean queued = jobEnqueuer.enqueue(persisted, payload).isPresent();
                return new WebhookResult(eventId, queued ? "QUEUED" : "ACCEPTED",
                        false, queued ? "Recovery processing queued" : "Verified event persisted");
            }

            WebhookResult result = processor.process(eventId, body, digest);
            if (persisted != null) {
                eventService.markProcessed(persisted.getId(), persisted.getIncidentId());
            }
            return result;
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            return processor.duplicate(eventId);
        } catch (IllegalArgumentException invalid) {
            if (eventService != null && eventService.isDuplicate(eventId)) {
                return processor.duplicate(eventId);
            }
            throw invalid;
        }
    }

    private boolean isDuplicate(String eventId) {
        return (eventService != null && eventService.isDuplicate(eventId))
                || processor.alreadyProcessed(eventId);
    }

    private WebhookEvent persist(String eventId, String eventType, byte[] body, String signature) {
        if (eventService == null) {
            return null;
        }
        return eventService.persist(eventId, eventType,
                new String(body, StandardCharsets.UTF_8), signature);
    }

    private String requestId() {
        String requestId = RequestContext.getRequestId();
        return requestId == null ? "unassigned" : requestId;
    }
}
