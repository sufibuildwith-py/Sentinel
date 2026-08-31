package com.sentinel.revenue.api;

public record DemoResetResponse(
        int incidentsReset,
        int eventsReset,
        boolean auditHistoryPreserved,
        String message) {
}
