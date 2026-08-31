package com.sentinel.revenue.api;

import com.sentinel.revenue.metrics.RevenueMetrics;
import com.sentinel.revenue.metrics.RevenueMetricsService;
import com.sentinel.revenue.metrics.FinancialAttribution;
import com.sentinel.revenue.metrics.FinancialAttributionService;
import com.sentinel.revenue.metrics.LostRevenueExplorer;
import com.sentinel.revenue.metrics.LostRevenueExplorerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/revenue")
public class RevenueMetricsController {
    private final RevenueMetricsService metrics;
    private final FinancialAttributionService attribution;
    private final LostRevenueExplorerService lostRevenue;
    public RevenueMetricsController(RevenueMetricsService metrics, FinancialAttributionService attribution,
                                    LostRevenueExplorerService lostRevenue) {
        this.metrics = metrics; this.attribution = attribution; this.lostRevenue = lostRevenue;
    }
    @GetMapping("/metrics")
    public ResponseEntity<RevenueMetrics> metrics() { return ResponseEntity.ok(metrics.metrics()); }
    @GetMapping("/financial-attribution")
    public ResponseEntity<FinancialAttribution> attribution() {
        return ResponseEntity.ok(attribution.attribution());
    }
    @GetMapping("/lost-revenue")
    public ResponseEntity<LostRevenueExplorer> lostRevenue() {
        return ResponseEntity.ok(lostRevenue.explore());
    }
}
