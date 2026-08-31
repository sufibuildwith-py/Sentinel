package com.sentinel.revenue.api;

import com.sentinel.revenue.health.*;
import com.sentinel.revenue.model.SystemicRecoveryIncident;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/revenue/payment-health")
public class PaymentHealthController {
    private final PaymentHealthAnalyzer analyzer;
    private final SystemicIncidentService systemic;
    private final ProviderDowntimeIngestionService downtimes;
    public PaymentHealthController(PaymentHealthAnalyzer analyzer, SystemicIncidentService systemic,
                                   ProviderDowntimeIngestionService downtimes) {
        this.analyzer = analyzer; this.systemic = systemic; this.downtimes = downtimes;
    }
    @GetMapping
    public ResponseEntity<PaymentHealthReport> health(@RequestParam(defaultValue = "ALL") String merchantId) {
        return ResponseEntity.ok(analyzer.analyze(merchantId, Instant.now()));
    }
    @PostMapping("/systemic-incidents")
    public ResponseEntity<SystemicRecoveryIncident> correlate(@RequestBody CorrelationRequest request) {
        PaymentHealthReport health = analyzer.analyze(request.merchantId(), Instant.now());
        return ResponseEntity.ok(systemic.correlate(request.merchantId(), health,
                request.incidentIds(), Instant.now()));
    }
    @PostMapping("/provider-downtimes/refresh")
    public ResponseEntity<Map<String, Integer>> refreshDowntimes() {
        return ResponseEntity.ok(Map.of("inserted", downtimes.refresh()));
    }
    public record CorrelationRequest(String merchantId, List<UUID> incidentIds) {
        public CorrelationRequest { incidentIds = List.copyOf(incidentIds); }
    }
}
