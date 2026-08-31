package com.sentinel.revenue.api;

import com.sentinel.revenue.service.RevenueOperationsReadService;
import com.sentinel.revenue.service.EvidenceCapsuleService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/revenue")
public class RevenueOperationsController {
    private final RevenueOperationsReadService reads;
    private final EvidenceCapsuleService capsules;
    public RevenueOperationsController(RevenueOperationsReadService reads,
                                       ObjectProvider<EvidenceCapsuleService> capsuleProvider) {
        this.reads = reads;
        this.capsules = capsuleProvider.getIfAvailable();
    }
    @GetMapping("/incidents") public ResponseEntity<List<IncidentSummaryView>> incidents() {
        return ResponseEntity.ok(reads.incidents());
    }
    @GetMapping("/incidents/{id}") public ResponseEntity<IncidentDetailView> incident(@PathVariable UUID id) {
        return ResponseEntity.ok(reads.incident(id));
    }
    @GetMapping("/approvals") public ResponseEntity<List<ApprovalQueueItem>> approvals() {
        return ResponseEntity.ok(reads.approvals());
    }
    @GetMapping("/incidents/{id}/evidence-capsule")
    public ResponseEntity<EvidenceCapsuleView> evidenceCapsule(@PathVariable UUID id) {
        if (capsules == null) throw new IllegalStateException("Evidence capsule service is unavailable");
        return ResponseEntity.ok(capsules.assemble(id));
    }
}
