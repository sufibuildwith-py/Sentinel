package com.sentinel.core.api;

import com.sentinel.core.llm.LlmRuntimeStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/diagnostics")
public class LlmDiagnosticsController {
    private final LlmRuntimeStatus status;

    public LlmDiagnosticsController(LlmRuntimeStatus status) { this.status = status; }

    @GetMapping("/llm")
    public ResponseEntity<Map<String, Object>> llm() { return ResponseEntity.ok(status.snapshot()); }
}
