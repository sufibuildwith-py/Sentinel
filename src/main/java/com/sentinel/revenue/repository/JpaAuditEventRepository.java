package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.AuditEvent;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaAuditEventRepository implements AuditEventRepository {
    private final EntityManager entityManager;

    public JpaAuditEventRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override
    public AuditEvent append(AuditEvent event) {
        if (event.getEventId() != null) {
            throw new IllegalArgumentException("Audit events are immutable and may only be appended once");
        }
        entityManager.persist(event);
        entityManager.flush();
        return event;
    }

    @Override
    public List<AuditEvent> findTrail(UUID incidentId) {
        return entityManager.createQuery("""
                        select event from AuditEvent event
                        where event.incident.incidentId = :incidentId
                        order by event.timestamp asc, event.eventId asc
                        """, AuditEvent.class)
                .setParameter("incidentId", incidentId)
                .getResultList();
    }
}
