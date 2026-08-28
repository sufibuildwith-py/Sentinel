package com.sentinel.revenue.repository;

import com.sentinel.revenue.detection.RuleOutcome;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.policy.PolicyEvaluation;
import com.sentinel.revenue.policy.PolicyRuleResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaAuditEventRepository.class)
class RecoveryActionConcurrencyTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired RevenueIncidentRepository incidents;
    @Autowired RecoveryPlanRepository plans;
    @Autowired RecoveryActionRepository actions;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void twoSimultaneousIdenticalRequestsCreateOnlyOneActiveAction() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        RecoveryPlan seeded = tx.execute(status -> {
            RevenueIncident incident = incidents.saveAndFlush(new RevenueIncident("UPI_DEGRADATION",
                    RevenueIncidentStatus.POLICY_REVIEW, "LOW", 50_000, Instant.now(),
                    List.of("p1"), List.of("c1"), List.of(), null, null));
            return plans.saveAndFlush(new RecoveryPlan(incident, RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK,
                    "same request", 1, 50_000, new BigDecimal("0.9500"), 40_000,
                    RiskLevel.LOW, Instant.now()));
        });
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> request = () -> {
                try {
                    return Boolean.TRUE.equals(tx.execute(status -> {
                        RecoveryPlan plan = plans.findById(seeded.getId()).orElseThrow();
                        RevenueIncident incident = incidents.findById(plan.getIncidentId()).orElseThrow();
                        ready.countDown();
                        try { start.await(5, TimeUnit.SECONDS); }
                        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new RuntimeException(exception); }
                        actions.saveAndFlush(RecoveryAction.fromPersistedPolicy(plan, incident, autoPolicy(),
                                50_000, Instant.now()));
                        return true;
                    }));
                } catch (RuntimeException duplicateRejected) {
                    return false;
                }
            };
            Future<Boolean> first = executor.submit(request);
            Future<Boolean> second = executor.submit(request);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
            assertThat(actions.findAllByIncidentIncidentId(seeded.getIncidentId())).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private PolicyEvaluation autoPolicy() {
        return new PolicyEvaluation(PolicyDecision.AUTO, List.of(new PolicyRuleResult(
                "ALL_GUARDS", RuleOutcome.PASS, "true", "==", "true", false, "fixture")), "AUTO");
    }
}
