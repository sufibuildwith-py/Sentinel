package com.sentinel.revenue.audit;

import com.sentinel.revenue.repository.AuditEventRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditTrailService {
    private final RevenueIncidentRepository incidents;
    private final AuditEventRepository audits;

    public AuditTrailService(RevenueIncidentRepository incidents, AuditEventRepository audits) {
        this.incidents = incidents;
        this.audits = audits;
    }

    @Transactional(readOnly = true)
    public List<AuditTrailEntry> trail(UUID incidentId) {
        if (!incidents.existsById(incidentId)) {
            throw new IllegalArgumentException("Revenue incident not found: " + incidentId);
        }
        return audits.findTrail(incidentId).stream().map(AuditTrailEntry::from).toList();
    }
}
