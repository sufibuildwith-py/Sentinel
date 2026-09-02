package com.sentinel.revenue.service;

import com.sentinel.revenue.dataset.SyntheticPaymentDatasetGenerator;
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
        for (int index = 0; index < WORKLOAD_CASES; index++) {
            SyntheticPaymentDatasetGenerator.Scenario scenario = index % 2 == 0
                    ? SyntheticPaymentDatasetGenerator.Scenario.UPI_DEGRADATION
                    : SyntheticPaymentDatasetGenerator.Scenario.PROVIDER_OUTAGE;
            demoRevenueService.injectScenario(scenario, "workload-%02d".formatted(index + 1));
        }
        log.info("Preloaded {} isolated synthetic Test Mode recovery cases", WORKLOAD_CASES);
    }
}
