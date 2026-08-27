package com.sentinel.revenue.api;

import java.util.UUID;

public record DemoIncidentSummary(
        UUID incidentId,
        String type,
        long amountAtRiskMinor,
        int affectedPaymentCount) {
}
