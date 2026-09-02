package com.sentinel.revenue.service;

import com.sentinel.revenue.repository.PaymentEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a small, truthful Test Mode workload for a fresh production environment.
 * The seed is idempotent and never invokes a provider or starts recovery execution.
 */
@Component
@Profile("prod")
@ConditionalOnProperty(name = "sentinel.demo.seed-workload", havingValue = "true")
public class DemoWorkloadSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoWorkloadSeeder.class);
    private static final int WORKLOAD_CASES = 10;

    private final DemoRevenueService demoRevenueService;
    private final PaymentEventRepository paymentEvents;

    public DemoWorkloadSeeder(DemoRevenueService demoRevenueService,
                              PaymentEventRepository paymentEvents) {
        this.demoRevenueService = demoRevenueService;
        this.paymentEvents = paymentEvents;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        boolean alreadySeeded = paymentEvents.findAll().stream()
                .anyMatch(event -> event.getMetadata().keySet().stream()
                        .anyMatch(key -> "workloadNamespace".equals(key)));
        if (alreadySeeded) {
            log.debug("Synthetic Test Mode workload already present; skipping seed");
            return;
        }
        String[][] profiles = {
                {"UPI DEGRADATION", "UPI", "HDFC", "UPI_ISSUER_UNAVAILABLE", "47512"},
                {"GATEWAY TIMEOUT", "CARD", "RAZORPAY_GATEWAY", "PAYMENT_TIMED_OUT", "38900"},
                {"INSUFFICIENT FUNDS", "CARD", "ICICI", "INSUFFICIENT_FUNDS", "27850"},
                {"PAYMENT DECLINED", "CARD", "SBI", "PAYMENT_DECLINED", "19600"},
                {"NETWORK ERROR", "NETBANKING", "AXIS", "NETWORK_ERROR", "32400"},
                {"API FAILURE", "WALLET", "RAZORPAY_GATEWAY", "RAZORPAY_API_FAILURE", "22100"},
                {"BAD REQUEST", "CARD", "KOTAK", "BAD_REQUEST_ERROR", "15800"},
                {"RAIL DEGRADED", "UPI", "ICICI", "PAYMENT_RAIL_DEGRADED", "41200"},
                {"RISK REVIEW", "CARD", "HDFC", "RISK_REVIEW_REQUIRED", "68500"},
                {"PROVIDER OUTAGE", "NETBANKING", "RAZORPAY_GATEWAY", "PROVIDER_UNAVAILABLE", "53600"}
        };
        for (int index = 0; index < WORKLOAD_CASES; index++) {
            String[] profile = profiles[index];
            demoRevenueService.injectOperationalIncident("workload-%02d".formatted(index + 1),
                    profile[1], profile[2], profile[3], Long.parseLong(profile[4]));
        }
        log.info("Preloaded {} isolated synthetic Test Mode recovery cases", WORKLOAD_CASES);
    }
}
