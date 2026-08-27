package com.sentinel.revenue.api;

import com.sentinel.revenue.investigation.InvestigationReport;
import com.sentinel.revenue.service.RevenueInvestigationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/revenue/incidents")
public class RevenueIncidentController {
    private final RevenueInvestigationService investigations;

    public RevenueIncidentController(RevenueInvestigationService investigations) {
        this.investigations = investigations;
    }

    @PostMapping("/{incidentId}/investigate")
    public ResponseEntity<InvestigationReport> investigate(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(investigations.investigate(incidentId));
    }
}
