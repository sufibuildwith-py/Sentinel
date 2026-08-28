package com.sentinel.revenue.audit;

import com.sentinel.revenue.model.AuditEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditTrailEntry(UUID eventId, Instant timestamp, String actor, String stage,
                              String narrative, BigDecimal confidence,
                              List<String> evidence, List<String> ruleTrace,
                              String policyResult, String externalResourceId) {
    public static AuditTrailEntry from(AuditEvent event) {
        String stage = event.getAgent() == null ? event.getAction() : event.getAgent();
        String narrative = event.getDecision() != null ? event.getDecision()
                : event.getOutcome() != null ? event.getOutcome()
                : event.getPreviousState() != null || event.getNewState() != null
                ? "%s → %s".formatted(event.getPreviousState(), event.getNewState())
                : event.getAction();
        return new AuditTrailEntry(event.getEventId(), event.getTimestamp(), event.getActor(),
                stage, narrative, event.getConfidence(), event.getInputEvidence(),
                event.getPolicyRulesEvaluated(), event.getPolicyResult(),
                event.getExternalResourceId());
    }
}
