package com.sentinel.revenue.api;

import com.sentinel.revenue.failurelab.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/failure-lab")
public class FailureLabController {
    private final FailureLabService failureLab;

    public FailureLabController(FailureLabService failureLab) { this.failureLab = failureLab; }

    @GetMapping("/scenarios")
    public ResponseEntity<List<FailureLabScenario>> scenarios() {
        return ResponseEntity.ok(failureLab.scenarios());
    }

    @PostMapping("/scenarios/{id}/run")
    public ResponseEntity<FailureLabResult> run(@PathVariable String id) {
        return ResponseEntity.ok(failureLab.run(id));
    }
}
