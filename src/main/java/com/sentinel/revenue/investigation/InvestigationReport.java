package com.sentinel.revenue.investigation;

import com.sentinel.revenue.model.RevenueIncidentStatus;

import java.util.UUID;

public record InvestigationReport(UUID incidentId, RevenueIncidentStatus status,
                                  TriageResult triage, AnalystFindings analyst,
                                  int similarHistoricalIncidents,
                                  RootCauseResult diagnosis) {
}
