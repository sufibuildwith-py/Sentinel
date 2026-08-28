package com.sentinel.revenue.service;

import com.sentinel.revenue.api.HumanDecisionRequest;
import com.sentinel.revenue.api.HumanDecisionResponse;
import com.sentinel.revenue.audit.AuditLogService;
import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.repository.RecoveryActionRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class HumanApprovalService {
    private final RecoveryActionRepository actions;
    private final RevenueIncidentRepository incidents;
    private final AuditLogService audit;
    private final RevenueIncidentStateMachine stateMachine = new RevenueIncidentStateMachine();

    public HumanApprovalService(RecoveryActionRepository actions,
                                RevenueIncidentRepository incidents,
                                AuditLogService audit) {
        this.actions = actions;
        this.incidents = incidents;
        this.audit = audit;
    }

    @Transactional
    public HumanDecisionResponse approve(UUID actionId, HumanDecisionRequest request) {
        RecoveryAction action = action(actionId);
        RevenueIncident incident = incident(action);
        action.approve(Instant.now());
        actions.saveAndFlush(action);
        transition(incident, RevenueIncidentStatus.APPROVED, request.actor(), request.reason());
        audit.append(incident, request.actor(), null, "HUMAN_APPROVED", List.of(request.reason()),
                null, "Human approved action " + actionId, List.of(), "HUMAN", null, null,
                "Approved; execution has not started");
        return new HumanDecisionResponse(actionId, action.getStatus(), incident.getStatus(),
                request.actor(), request.reason());
    }

    @Transactional
    public HumanDecisionResponse reject(UUID actionId, HumanDecisionRequest request) {
        RecoveryAction action = action(actionId);
        RevenueIncident incident = incident(action);
        action.reject();
        actions.saveAndFlush(action);
        transition(incident, RevenueIncidentStatus.STOPPED, request.actor(), request.reason());
        audit.append(incident, request.actor(), null, "HUMAN_REJECTED", List.of(request.reason()),
                null, "Human rejected action " + actionId, List.of(), "DENY", null, null,
                "Rejected and stopped");
        return new HumanDecisionResponse(actionId, action.getStatus(), incident.getStatus(),
                request.actor(), request.reason());
    }

    private RecoveryAction action(UUID id) {
        return actions.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Recovery action not found: " + id));
    }
    private RevenueIncident incident(RecoveryAction action) {
        return incidents.findById(action.getIncidentId()).orElseThrow(() ->
                new IllegalStateException("Recovery action has no incident"));
    }
    private void transition(RevenueIncident incident, RevenueIncidentStatus target,
                            String actor, String reason) {
        RevenueIncidentStatus previous = incident.getStatus();
        incident.transitionTo(stateMachine.transition(previous, target));
        incidents.saveAndFlush(incident);
        audit.append(incident, actor, null, "STATE_TRANSITION", List.of(reason), null,
                reason, List.of(), null, previous, target, reason);
    }
}
