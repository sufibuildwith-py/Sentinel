package com.sentinel.revenue.api;

import com.sentinel.revenue.audit.AuditTrailEntry;
import com.sentinel.revenue.audit.AuditTrailService;
import com.sentinel.revenue.planning.RecoveryPlanningResult;
import com.sentinel.revenue.planning.RecoveryPlanningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/revenue/incidents")
public class RecoveryPlanningController {
    private final RecoveryPlanningService planning;
    private final AuditTrailService auditTrail;

    public RecoveryPlanningController(RecoveryPlanningService planning, AuditTrailService auditTrail) {
        this.planning = planning;
        this.auditTrail = auditTrail;
    }

    @PostMapping("/{incidentId}/plan")
    public ResponseEntity<RecoveryPlanningResult> plan(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(planning.plan(incidentId));
    }

    @GetMapping("/{incidentId}/audit-trail")
    public ResponseEntity<List<AuditTrailEntry>> auditTrail(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(auditTrail.trail(incidentId));
    }
}
