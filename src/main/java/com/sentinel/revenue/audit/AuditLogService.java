package com.sentinel.revenue.audit;

import com.sentinel.revenue.model.AuditEvent;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.repository.AuditEventRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class AuditLogService {
    private final AuditEventRepository repository;

    public AuditLogService(AuditEventRepository repository) { this.repository = repository; }

    public AuditEvent append(RevenueIncident incident, String actor, String agent, String action,
                             List<String> evidence, BigDecimal confidence, String decision,
                             List<String> policyRules, String policyResult,
                             RevenueIncidentStatus previousState, RevenueIncidentStatus newState,
                             String outcome) {
        return repository.append(new AuditEvent(incident, Instant.now(), actor, agent, action,
                evidence, confidence, decision, policyRules, policyResult, null,
                previousState, newState, outcome));
    }

    public AuditEvent appendExternal(RevenueIncident incident, String actor, String action,
                                     List<String> evidence, String decision,
                                     String externalResourceId, String outcome) {
        return repository.append(new AuditEvent(incident, Instant.now(), actor, null, action,
                evidence, null, decision, List.of(), null, externalResourceId,
                null, null, outcome));
    }
}
