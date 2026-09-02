package com.sentinel.revenue.api;

import com.sentinel.revenue.service.DemoRevenueService;
import com.sentinel.revenue.dataset.SyntheticPaymentDatasetGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    private final DemoRevenueService demoRevenueService;
    private final ObjectProvider<com.sentinel.revenue.service.DemoWorkloadSeeder> workloadSeeder;

    public DemoController(DemoRevenueService demoRevenueService,
                          ObjectProvider<com.sentinel.revenue.service.DemoWorkloadSeeder> workloadSeeder) {
        this.demoRevenueService = demoRevenueService;
        this.workloadSeeder = workloadSeeder;
    }

    @PostMapping("/reset")
    public ResponseEntity<DemoResetResponse> reset() {
        return ResponseEntity.ok(demoRevenueService.resetSyntheticState());
    }

    @PostMapping("/ensure-workload")
    public ResponseEntity<java.util.Map<String, Object>> ensureWorkload() {
        var seeder = workloadSeeder.getIfAvailable();
        int created = seeder == null ? 0 : seeder.ensureSeeded();
        return ResponseEntity.ok(java.util.Map.of("created", created, "idempotent", true));
    }

    @PostMapping("/inject/upi-outage")
    public ResponseEntity<DemoInjectionResponse> injectUpiOutage() {
        return ResponseEntity.ok(demoRevenueService.injectUpiOutage());
    }

    @PostMapping("/inject/{scenario}")
    public ResponseEntity<DemoInjectionResponse> injectScenario(@PathVariable String scenario) {
        SyntheticPaymentDatasetGenerator.Scenario selected = switch (scenario.toLowerCase()) {
            case "upi-outage", "upi-degradation" -> SyntheticPaymentDatasetGenerator.Scenario.UPI_DEGRADATION;
            case "provider-outage", "gateway-outage" -> SyntheticPaymentDatasetGenerator.Scenario.PROVIDER_OUTAGE;
            case "insufficient-funds" -> SyntheticPaymentDatasetGenerator.Scenario.INSUFFICIENT_FUNDS;
            case "already-paid" -> SyntheticPaymentDatasetGenerator.Scenario.ALREADY_PAID;
            case "high-value" -> SyntheticPaymentDatasetGenerator.Scenario.HIGH_VALUE;
            case "customer-abandonment" -> SyntheticPaymentDatasetGenerator.Scenario.CUSTOMER_ABANDONMENT;
            default -> throw new IllegalArgumentException("Unknown demo scenario: " + scenario);
        };
        return ResponseEntity.ok(demoRevenueService.injectScenario(selected));
    }
}
