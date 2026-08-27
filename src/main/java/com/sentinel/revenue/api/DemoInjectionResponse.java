package com.sentinel.revenue.api;

import java.util.List;

public record DemoInjectionResponse(
        BatchIngestionSummary ingestion,
        int incidentsCreated,
        List<DemoIncidentSummary> incidents) {

    public DemoInjectionResponse {
        incidents = List.copyOf(incidents);
    }
}
