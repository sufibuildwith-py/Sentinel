package com.sentinel.revenue.api;

import com.sentinel.revenue.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentDetailView(IncidentSummaryView incident, List<FindingView> findings,
                                 PlanView plan, ActionView action) {
    public IncidentDetailView { findings = List.copyOf(findings); }
    public record FindingView(String source, String summary, BigDecimal confidence,
                              List<String> evidence, Instant createdAt) { }
    public record PlanView(UUID planId, RecoveryStrategy strategy, String reason,
                           long targetAmountMinor, BigDecimal confidence, RiskLevel riskLevel) { }
    public record ActionView(UUID actionId, RecoveryActionStatus status, PolicyDecision policyDecision,
                             long amountMinor, String currency, String providerId,
                             String referenceId, String shortUrl, String providerStatus,
                             int executionAttempts, Instant approvedAt, Instant executedAt) { }
}
