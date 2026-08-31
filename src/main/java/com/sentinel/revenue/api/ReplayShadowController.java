package com.sentinel.revenue.api;
import com.sentinel.revenue.model.PolicyReplaySnapshot;
import com.sentinel.revenue.replay.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/revenue/replay")
public class ReplayShadowController {
 private final PolicyReplayService replay; private final ShadowDecisionEngine shadow;
 public ReplayShadowController(PolicyReplayService replay,ShadowDecisionEngine shadow){this.replay=replay;this.shadow=shadow;}
 @PostMapping("/snapshots") public ResponseEntity<PolicyReplaySnapshot> capture(@RequestBody ReplayCaptureCommand c){return ResponseEntity.ok(replay.capture(c));}
 @PostMapping("/snapshots/{id}") public ResponseEntity<PolicyReplayResult> replay(@PathVariable UUID id){return ResponseEntity.ok(replay.replay(id));}
 @PostMapping("/snapshots/{id}/shadow") public ResponseEntity<ShadowComparison> shadow(@PathVariable UUID id,@RequestBody ShadowCandidate c){return ResponseEntity.ok(shadow.compare(id,c));}
}
