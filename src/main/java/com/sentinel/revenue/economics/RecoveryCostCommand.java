package com.sentinel.revenue.economics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecoveryCostCommand(UUID incidentId, UUID recoveryActionId, UUID decisionId,
                                  RecoveryCostCategory category, BigDecimal amountMinor,
                                  String currency, String source, String calculationMethod,
                                  EconomicEvidenceQuality evidenceQuality, String version,
                                  Instant occurredAt) { }
