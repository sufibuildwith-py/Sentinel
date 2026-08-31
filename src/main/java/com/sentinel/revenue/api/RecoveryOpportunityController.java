package com.sentinel.revenue.api;
import com.sentinel.revenue.opportunity.*;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/revenue/incidents")
public class RecoveryOpportunityController {
    private final RevenueIncidentRepository incidents;
    private final RecoveryOpportunityEngine opportunities;
    public RecoveryOpportunityController(RevenueIncidentRepository incidents, RecoveryOpportunityEngine opportunities) {
        this.incidents = incidents; this.opportunities = opportunities;
    }
    @PostMapping("/{id}/opportunities")
    public ResponseEntity<RecoveryOpportunityDecision> evaluate(@PathVariable UUID id) {
        return ResponseEntity.ok(opportunities.evaluate(incidents.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Revenue incident not found: " + id)), null));
    }
}
