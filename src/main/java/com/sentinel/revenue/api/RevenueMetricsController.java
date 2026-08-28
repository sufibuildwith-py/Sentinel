package com.sentinel.revenue.api;

import com.sentinel.revenue.metrics.RevenueMetrics;
import com.sentinel.revenue.metrics.RevenueMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/revenue")
public class RevenueMetricsController {
    private final RevenueMetricsService metrics;
    public RevenueMetricsController(RevenueMetricsService metrics) { this.metrics = metrics; }
    @GetMapping("/metrics")
    public ResponseEntity<RevenueMetrics> metrics() { return ResponseEntity.ok(metrics.metrics()); }
}
