package com.sentinel.revenue.portfolio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HumanReviewCandidate(UUID incidentId, long amountAtRiskMinor,
                                   BigDecimal expectedIncrementalValueMinor, BigDecimal uncertainty,
                                   String risk, Instant decisionDeadline, int estimatedReviewMinutes,
                                   String automationStopReason, String recommendedAction,
                                   List<String> alternatives, List<String> evidence) {
    public HumanReviewCandidate {
        alternatives = List.copyOf(alternatives); evidence = List.copyOf(evidence);
    }
}
