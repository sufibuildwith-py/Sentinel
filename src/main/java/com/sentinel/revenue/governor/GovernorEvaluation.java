package com.sentinel.revenue.governor;

import java.util.List;
import java.util.UUID;

public record GovernorEvaluation(UUID decisionId, boolean allowed, long allowedValueMinor,
                                 ExecutionEnvelope envelope, List<String> violations) {
    public GovernorEvaluation { violations = List.copyOf(violations); }
}
