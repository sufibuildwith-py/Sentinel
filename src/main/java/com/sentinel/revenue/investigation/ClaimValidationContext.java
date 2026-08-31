package com.sentinel.revenue.investigation;

import com.sentinel.revenue.model.IncidentFinding;
import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RecoveryOutcome;

import java.time.Instant;
import java.util.List;

public record ClaimValidationContext(List<IncidentFinding> evidence,
                                     RecoveryAction action,
                                     RecoveryOutcome outcome,
                                     Instant now) {
    public ClaimValidationContext {
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
        now = now == null ? Instant.now() : now;
    }
}
