package com.sentinel.revenue.api;

import com.sentinel.revenue.governor.*;
import com.sentinel.revenue.model.RecoveryBatch;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/revenue/governor")
public class RecoveryGovernorController {
    private final KillSwitchService killSwitches;
    private final RecoveryBatchService batches;
    public RecoveryGovernorController(KillSwitchService killSwitches, RecoveryBatchService batches) {
        this.killSwitches = killSwitches; this.batches = batches;
    }
    @GetMapping("/kill-switches")
    public ResponseEntity<Map<KillSwitch, Boolean>> killSwitches() {
        return ResponseEntity.ok(killSwitches.states());
    }
    @PutMapping("/kill-switches/{name}")
    public ResponseEntity<Void> setKillSwitch(@PathVariable KillSwitch name,
                                              @RequestBody KillSwitchRequest request) {
        killSwitches.set(name, request.enabled(), request.reason());
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/batches")
    public ResponseEntity<RecoveryBatch> createBatch(@RequestBody BatchRequest request) {
        return ResponseEntity.ok(batches.create(request.strategy(), request.incidentIds()));
    }
    @PostMapping("/batches/{id}/expand")
    public ResponseEntity<RecoveryBatch> expand(@PathVariable UUID id) {
        return ResponseEntity.ok(batches.expandAfterReconciliation(id));
    }
    public record KillSwitchRequest(boolean enabled, String reason) { }
    public record BatchRequest(String strategy, List<UUID> incidentIds) {
        public BatchRequest { incidentIds = List.copyOf(incidentIds); }
    }
}
