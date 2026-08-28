package com.sentinel.revenue.api;

import com.sentinel.revenue.model.RecoveryActionStatus;
import com.sentinel.revenue.model.RevenueIncidentStatus;

import java.util.UUID;

public record HumanDecisionResponse(UUID actionId, RecoveryActionStatus actionStatus,
                                    RevenueIncidentStatus incidentStatus,
                                    String actor, String reason) {
}
