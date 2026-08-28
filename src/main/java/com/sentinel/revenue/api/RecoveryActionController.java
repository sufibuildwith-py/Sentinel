package com.sentinel.revenue.api;

import com.sentinel.revenue.service.HumanApprovalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/revenue/actions")
public class RecoveryActionController {
    private final HumanApprovalService approvals;

    public RecoveryActionController(HumanApprovalService approvals) { this.approvals = approvals; }

    @PostMapping("/{actionId}/approve")
    public ResponseEntity<HumanDecisionResponse> approve(@PathVariable UUID actionId,
                                                         @Valid @RequestBody HumanDecisionRequest request) {
        return ResponseEntity.ok(approvals.approve(actionId, request));
    }

    @PostMapping("/{actionId}/reject")
    public ResponseEntity<HumanDecisionResponse> reject(@PathVariable UUID actionId,
                                                        @Valid @RequestBody HumanDecisionRequest request) {
        return ResponseEntity.ok(approvals.reject(actionId, request));
    }
}
