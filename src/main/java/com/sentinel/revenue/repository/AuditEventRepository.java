package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.AuditEvent;
import java.util.List;
import java.util.UUID;

/** Append and chronological read are the only operations intentionally exposed. */
public interface AuditEventRepository {
    AuditEvent append(AuditEvent event);
    List<AuditEvent> findTrail(UUID incidentId);
}
