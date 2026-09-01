package com.sentinel.revenue.execution;

import com.sentinel.revenue.audit.AuditLogService;
import com.sentinel.revenue.detection.RuleOutcome;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.policy.PolicyEvaluation;
import com.sentinel.revenue.policy.PolicyRuleResult;
import com.sentinel.revenue.governor.DynamicRecoveryGovernor;
import com.sentinel.revenue.governor.KillSwitchService;
import com.sentinel.revenue.governor.RecoverySafetyGovernor;
import com.sentinel.revenue.governor.RecoverySafetyProperties;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "sentinel.razorpay.enabled=true", "sentinel.razorpay.key-id=rzp_test_concurrency",
        "sentinel.razorpay.key-secret=test-secret", "sentinel.razorpay.base-url=http://localhost",
        "sentinel.razorpay.connect-timeout=1s", "sentinel.razorpay.request-timeout=1s",
        "sentinel.razorpay.action-expiry=30m", "sentinel.razorpay.link-expiry=24h",
        "sentinel.razorpay.maximum-attempts=2", "sentinel.razorpay.circuit-breaker-failure-rate=50",
        "sentinel.razorpay.circuit-breaker-minimum-calls=2", "sentinel.razorpay.circuit-breaker-window-size=4",
        "sentinel.razorpay.circuit-breaker-open-duration=30s", "sentinel.razorpay.notifications-enabled=false",
        "sentinel.governor.max-total-value-minor=10000000", "sentinel.governor.max-incidents=100",
        "sentinel.governor.max-value-per-incident-minor=100000", "sentinel.governor.max-provider-calls-per-minute=30",
        "sentinel.governor.max-customer-contacts=20", "sentinel.governor.max-retry-count=3",
        "sentinel.governor.max-concurrent-jobs=10", "sentinel.governor.max-tool-failure-rate=0.25",
        "sentinel.governor.max-unreconciled-value-minor=500000", "sentinel.governor.canary-size=2",
        "sentinel.governor.required-reconciled-count=2"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({RecoveryExecutionService.class, AuditLogService.class, JpaAuditEventRepository.class,
        RecoverySafetyGovernor.class, KillSwitchService.class, DynamicRecoveryGovernor.class})
@EnableConfigurationProperties({RazorpayProperties.class, RecoverySafetyProperties.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RecoveryExecutionConcurrencyTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired RecoveryExecutionService service;
    @Autowired RevenueIncidentRepository incidents;
    @Autowired RecoveryPlanRepository plans;
    @Autowired RecoveryActionRepository actions;
    @Autowired PaymentEventRepository payments;
    @Autowired RecoveryOutcomeRepository outcomes;
    @Autowired RecoveryGovernorDecisionRepository governorDecisions;
    @Autowired AuditEventRepository auditEvents;
    @Autowired RecoveryJobRepository jobs;
    @MockBean RazorpayGateway gateway;

    @Test void concurrentRequestsIssueOneProviderCreate() throws Exception {
        seedResetHistoryThatWouldExhaustTheEnvelopeIfCounted();
        UUID incidentId = seed();
        when(gateway.findPaymentLinkByReference(any())).thenReturn(java.util.Optional.empty());
        when(gateway.createPaymentLink(any())).thenAnswer(call -> {
            Thread.sleep(100);
            PaymentLinkCommand command = call.getArgument(0);
            return new PaymentLinkResource("plink_concurrent", command.referenceId(),
                    "https://rzp.io/i/concurrent", "created");
        });
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<RecoveryExecutionResponse> request = () -> { start.await(); return service.execute(incidentId); };
            Future<RecoveryExecutionResponse> first = pool.submit(request);
            Future<RecoveryExecutionResponse> second = pool.submit(request);
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .allSatisfy(response -> assertThat(response.providerId()).isEqualTo("plink_concurrent"));
            verify(gateway, times(1)).createPaymentLink(any());
            assertThat(actions.findAllByIncidentIncidentId(incidentId)).singleElement()
                    .satisfies(action -> {
                        assertThat(action.getStatus()).isEqualTo(RecoveryActionStatus.EXECUTED);
                        assertThat(action.getExternalResourceId()).isEqualTo("plink_concurrent");
                        assertThat(action.getExecutionAttempts()).isEqualTo(1);
                    });
            assertThat(governorDecisions.findAllByIncidentIdOrderByCreatedAtAsc(incidentId)).singleElement()
                    .satisfies(decision -> {
                        assertThat(decision.isAllowed()).isTrue();
                        assertThat(decision.getViolations()).isEmpty();
                        assertThat(decision.getAllowedValueMinor()).isEqualTo(12_345);
                    });
            assertThat(outcomes.findAllByIncidentIncidentId(incidentId)).isEmpty();
            assertThat(incidents.findById(incidentId)).get()
                    .extracting(RevenueIncident::getStatus).isEqualTo(RevenueIncidentStatus.MONITORING);
            assertThat(auditEvents.findTrail(incidentId)).extracting(AuditEvent::getAction)
                    .contains("BLAST_RADIUS_EVALUATED", "EXECUTION_CLAIMED", "PROVIDER_REQUEST", "EXECUTION_SUCCESS");
        } finally { pool.shutdownNow(); }
    }

    private void seedResetHistoryThatWouldExhaustTheEnvelopeIfCounted() {
        Instant now = Instant.now();
        RevenueIncident reset = new RevenueIncident("RESET_HISTORY", RevenueIncidentStatus.APPROVED,
                "LOW", 9_900_000, now, List.of("old-payment"), List.of("old-customer"),
                List.of(), null, null);
        reset.markDemoReset(now);
        incidents.saveAndFlush(reset);
        RecoveryPlan oldPlan = plans.saveAndFlush(new RecoveryPlan(reset,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "preserved reset history", 1, 9_900_000,
                new BigDecimal("0.9500"), 1, RiskLevel.LOW, now));
        PolicyRuleResult trace = new PolicyRuleResult("TEST", RuleOutcome.PASS, "true", "==", "true", false, "test");
        actions.saveAndFlush(RecoveryAction.fromPersistedPolicy(oldPlan, reset,
                new PolicyEvaluation(PolicyDecision.AUTO, List.of(trace), "test"), 9_900_000, now));
        RecoveryJob oldJob = jobs.saveAndFlush(new RecoveryJob(reset.getIncidentId(), null,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK.name(), 3, now));
        oldJob.markRunning(now);
        oldJob.markFailed("preserved historical provider failure", now);
        jobs.saveAndFlush(oldJob);
    }

    private UUID seed() {
        RevenueIncident incident = incidents.saveAndFlush(new RevenueIncident("UPI_DEGRADATION",
                RevenueIncidentStatus.APPROVED, "LOW", 50_000, Instant.now(), List.of("local_failed_1"),
                List.of("customer_0182"), List.of(), null, null));
        payments.saveAndFlush(new PaymentEvent("local_failed_1", "order_1", "customer_0182", 12_345,
                "INR", "UPI", "Bank X", "FAILED", "UPI_DOWN", "failed", Instant.now(),
                1, null, 0, null, Map.of()));
        RecoveryPlan plan = plans.saveAndFlush(new RecoveryPlan(incident,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "UPI degraded", 1, 50_000,
                new BigDecimal("0.9500"), 40_000, RiskLevel.LOW, Instant.now()));
        PolicyRuleResult trace = new PolicyRuleResult("TEST", RuleOutcome.PASS, "true", "==", "true", false, "test");
        actions.saveAndFlush(RecoveryAction.fromPersistedPolicy(plan, incident,
                new PolicyEvaluation(PolicyDecision.AUTO, List.of(trace), "test"), 50_000, Instant.now()));
        return incident.getIncidentId();
    }
}
