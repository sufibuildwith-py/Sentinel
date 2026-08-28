package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.WebhookSecurityEvent;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class JpaWebhookSecurityEventRepository implements WebhookSecurityEventRepository {
    private final EntityManager entityManager;
    public JpaWebhookSecurityEventRepository(EntityManager entityManager) { this.entityManager = entityManager; }
    @Override public WebhookSecurityEvent append(WebhookSecurityEvent event) {
        if (event.getId() != null) throw new IllegalArgumentException("Security audit events are append-only");
        entityManager.persist(event); entityManager.flush(); return event;
    }
}
