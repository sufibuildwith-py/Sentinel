package com.sentinel.revenue.api;

import com.sentinel.revenue.execution.RecoveryExecutionResponse;
import com.sentinel.revenue.execution.RecoveryExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/revenue")
public class RecoveryExecutionController {
    private final RecoveryExecutionService execution;
    public RecoveryExecutionController(RecoveryExecutionService execution) { this.execution = execution; }

    @PostMapping("/incidents/{incidentId}/execute")
    public ResponseEntity<RecoveryExecutionResponse> execute(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(execution.execute(incidentId));
    }
    @PostMapping("/actions/{actionId}/cancel")
    public ResponseEntity<RecoveryExecutionResponse> cancel(@PathVariable UUID actionId) {
        return ResponseEntity.ok(execution.cancel(actionId));
    }
    @PostMapping("/actions/{actionId}/notify/{medium}")
    public ResponseEntity<Void> notify(@PathVariable UUID actionId, @PathVariable String medium) {
        execution.notify(actionId, medium); return ResponseEntity.noContent().build();
    }
}
