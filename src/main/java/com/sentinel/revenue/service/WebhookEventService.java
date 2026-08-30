package com.sentinel.revenue.service;

import com.sentinel.revenue.model.WebhookEvent;
import com.sentinel.revenue.repository.WebhookEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookEventService {

    private final WebhookEventRepository repository;

    public WebhookEventService(WebhookEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WebhookEvent persist(String eventId, String eventType,
                                String payload, String signature) {
        if (repository.existsByEventId(eventId)) {
            throw new IllegalArgumentException("Webhook event already exists: " + eventId);
        }
        try {
            return repository.saveAndFlush(new WebhookEvent(
                    eventId, eventType, payload, signature,
                    true, false, null, Instant.now(), null));
        } catch (DataIntegrityViolationException duplicate) {
            throw new IllegalArgumentException("Webhook event already exists: " + eventId, duplicate);
        }
    }

    @Transactional(readOnly = true)
    public boolean isDuplicate(String eventId) {
        return repository.existsByEventId(eventId);
    }

    @Transactional
    public WebhookEvent markProcessed(UUID id, UUID incidentId) {
        WebhookEvent event = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook event not found: " + id));
        event.markProcessed(incidentId, Instant.now());
        return repository.saveAndFlush(event);
    }

    @Transactional
    public WebhookEvent associateIncident(UUID id, UUID incidentId) {
        WebhookEvent event = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook event not found: " + id));
        event.associateIncident(incidentId);
        return repository.saveAndFlush(event);
    }

    @Transactional(readOnly = true)
    public List<WebhookEvent> findUnprocessed() {
        return repository.findByProcessedFalseOrderByReceivedAtAsc();
    }
}
