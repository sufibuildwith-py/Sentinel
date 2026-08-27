package com.sentinel.revenue.investigation;

import com.sentinel.revenue.model.RecoveryOutcomeStatus;
import com.sentinel.revenue.model.RecoveryStrategy;

import java.util.UUID;

public record SimilarHistoricalIncident(UUID id, String rootCause,
                                        RecoveryStrategy strategy,
                                        RecoveryOutcomeStatus outcome,
                                        long recoveredAmountMinor,
                                        Double recoveryRate, double similarity) {
}
