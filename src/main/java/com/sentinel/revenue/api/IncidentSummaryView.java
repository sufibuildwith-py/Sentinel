package com.sentinel.revenue.api;

import com.sentinel.revenue.model.*;
import java.time.Instant;
import java.util.UUID;

public record IncidentSummaryView(UUID incidentId, String type, RevenueIncidentStatus status,
                                  String severity, long amountAtRiskMinor, Instant detectedAt,
                                  int affectedPaymentCount, int affectedCustomerCount,
                                  RecoveryStrategy strategy, PolicyDecision policyDecision,
                                  RecoveryActionStatus actionStatus, RecoveryOutcomeStatus latestOutcome,
                                  long recoveredAmountMinor) { }
