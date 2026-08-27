package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findAllByIncidentIncidentIdOrderByTimestampAsc(UUID incidentId);
}
