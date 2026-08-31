package com.sentinel.revenue.api;

import com.sentinel.revenue.metrics.RevenueMetrics;
import com.sentinel.revenue.metrics.RevenueMetricsService;
import com.sentinel.revenue.metrics.FinancialAttribution;
import com.sentinel.revenue.metrics.FinancialAttributionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/revenue")
public class RevenueMetricsController {
    private final RevenueMetricsService metrics;
    private final FinancialAttributionService attribution;
    public RevenueMetricsController(RevenueMetricsService metrics, FinancialAttributionService attribution) {
        this.metrics = metrics; this.attribution = attribution;
    }
    @GetMapping("/metrics")
    public ResponseEntity<RevenueMetrics> metrics() { return ResponseEntity.ok(metrics.metrics()); }
    @GetMapping("/financial-attribution")
    public ResponseEntity<FinancialAttribution> attribution() {
        return ResponseEntity.ok(attribution.attribution());
    }
}
