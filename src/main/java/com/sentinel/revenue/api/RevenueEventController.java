package com.sentinel.revenue.api;

import com.sentinel.revenue.service.PaymentEventIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/revenue/events")
public class RevenueEventController {

    private final PaymentEventIngestionService ingestionService;

    public RevenueEventController(PaymentEventIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchIngestionSummary> ingestBatch(
            @Valid @RequestBody PaymentEventBatchRequest request) {
        return ResponseEntity.ok(ingestionService.ingest(request));
    }
}
