package com.sentinel.revenue.api;

import com.sentinel.revenue.service.DemoRevenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    private final DemoRevenueService demoRevenueService;

    public DemoController(DemoRevenueService demoRevenueService) {
        this.demoRevenueService = demoRevenueService;
    }

    @PostMapping("/reset")
    public ResponseEntity<DemoResetResponse> reset() {
        return ResponseEntity.ok(demoRevenueService.resetSyntheticState());
    }

    @PostMapping("/inject/upi-outage")
    public ResponseEntity<DemoInjectionResponse> injectUpiOutage() {
        return ResponseEntity.ok(demoRevenueService.injectUpiOutage());
    }
}
