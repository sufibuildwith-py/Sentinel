package com.sentinel.revenue.api;

import com.sentinel.revenue.service.RevenueOperationsReadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/revenue")
public class RevenueOperationsController {
    private final RevenueOperationsReadService reads;
    public RevenueOperationsController(RevenueOperationsReadService reads) { this.reads = reads; }
    @GetMapping("/incidents") public ResponseEntity<List<IncidentSummaryView>> incidents() {
        return ResponseEntity.ok(reads.incidents());
    }
    @GetMapping("/incidents/{id}") public ResponseEntity<IncidentDetailView> incident(@PathVariable UUID id) {
        return ResponseEntity.ok(reads.incident(id));
    }
    @GetMapping("/approvals") public ResponseEntity<List<ApprovalQueueItem>> approvals() {
        return ResponseEntity.ok(reads.approvals());
    }
}
