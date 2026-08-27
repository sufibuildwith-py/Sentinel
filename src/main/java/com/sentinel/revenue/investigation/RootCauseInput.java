package com.sentinel.revenue.investigation;

import com.sentinel.revenue.model.RevenueIncident;

import java.util.List;

public record RootCauseInput(RevenueIncident incident, TriageResult triage,
                             AnalystFindings analyst,
                             List<SimilarHistoricalIncident> memory) {
    public RootCauseInput { memory = List.copyOf(memory); }
}
