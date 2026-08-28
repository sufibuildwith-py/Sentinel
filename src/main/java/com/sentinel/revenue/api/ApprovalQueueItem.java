package com.sentinel.revenue.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ApprovalQueueItem(UUID actionId, UUID incidentId, String incidentType,
                                long amountMinor, BigDecimal confidence, String reason,
                                List<String> failedPolicyRules) {
    public ApprovalQueueItem { failedPolicyRules = List.copyOf(failedPolicyRules); }
}
