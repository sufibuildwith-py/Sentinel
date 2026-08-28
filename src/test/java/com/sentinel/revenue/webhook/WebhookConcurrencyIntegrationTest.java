package com.sentinel.revenue.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.revenue.audit.AuditLogService;
import com.sentinel.revenue.detection.RuleOutcome;
import com.sentinel.revenue.metrics.RevenueMetricsService;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.policy.PolicyEvaluation;
import com.sentinel.revenue.policy.PolicyRuleResult;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "sentinel.razorpay.webhook.secret=integration_secret"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnableConfigurationProperties(RazorpayWebhookProperties.class)
@Import({WebhookRequestHandler.class, WebhookSignatureVerifier.class, WebhookSecurityAuditService.class,
        WebhookOutcomeProcessor.class, AuditLogService.class, RevenueMetricsService.class,
        JpaAuditEventRepository.class, JpaWebhookSecurityEventRepository.class,
        WebhookConcurrencyIntegrationTest.JsonConfig.class})
class WebhookConcurrencyIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @TestConfiguration static class JsonConfig { @Bean ObjectMapper objectMapper() { return new ObjectMapper(); } }
    @Autowired WebhookRequestHandler handler;
    @Autowired ProcessedWebhookEventRepository webhooks;
    @Autowired RecoveryOutcomeRepository outcomes;
    @Autowired HistoricalIncidentRepository history;
    @Autowired RevenueIncidentRepository incidents;
    @Autowired RecoveryActionRepository actions;
    @Autowired RecoveryPlanRepository plans;
    @Autowired RevenueMetricsService metrics;
    private final TransactionTemplate tx;
    WebhookConcurrencyIntegrationTest(@Autowired PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Test void concurrentDuplicatePaidDeliveryChangesRevenueAndMemoryOnce() throws Exception {
        UUID incidentId = tx.execute(ignored -> seed("plink_concurrent"));
        byte[] body = payload("plink_concurrent", 10_000, 10_000, "INR");
        String signature = WebhookSignatureVerifierTest.sign(body, "integration_secret");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<WebhookResult> request = () -> { start.await(); return handler.handle(body, signature, "evt_same"); };
            Future<WebhookResult> first = pool.submit(request); Future<WebhookResult> second = pool.submit(request);
            start.countDown();
            List<WebhookResult> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(results).extracting(WebhookResult::disposition).containsExactlyInAnyOrder("APPLIED", "DUPLICATE");
            assertThat(webhooks.count()).isEqualTo(1);
            assertThat(outcomes.count()).isEqualTo(1);
            assertThat(history.count()).isEqualTo(1);
            assertThat(incidents.findById(incidentId).orElseThrow().getStatus()).isEqualTo(RevenueIncidentStatus.RECOVERED);
            assertThat(metrics.metrics().recoveredRevenueMinor()).isEqualTo(10_000);
        } finally { pool.shutdownNow(); }
    }

    @Test void signedMoneyMismatchIsDurablyRejectedWithoutOutcomeMutation() throws Exception {
        UUID incidentId = tx.execute(ignored -> seed("plink_mismatch"));
        byte[] body = payload("plink_mismatch", 11_000, 10_000, "INR");
        WebhookResult result = handler.handle(body,
                WebhookSignatureVerifierTest.sign(body, "integration_secret"), "evt_mismatch");
        assertThat(result.disposition()).isEqualTo("REJECTED");
        assertThat(outcomes.findAllByIncidentIncidentId(incidentId)).isEmpty();
        assertThat(webhooks.findByEventId("evt_mismatch")).hasValueSatisfying(event ->
                assertThat(event.getProcessingError()).isEqualTo("AMOUNT_MISMATCH"));
    }

    private UUID seed(String linkId) {
        RevenueIncident incident = incidents.saveAndFlush(new RevenueIncident("UPI_DEGRADATION",
                RevenueIncidentStatus.MONITORING, "HIGH", 20_000, Instant.now(), List.of("p1"),
                List.of("c1"), List.of(), "UPI issuer degradation", null));
        RecoveryPlan plan = plans.saveAndFlush(new RecoveryPlan(incident,
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, "alternative", 1, 10_000,
                new BigDecimal("0.9000"), 8_000, RiskLevel.LOW, Instant.now()));
        PolicyRuleResult trace = new PolicyRuleResult("TEST", RuleOutcome.PASS, "true", "==", "true", false, "test");
        RecoveryAction action = RecoveryAction.fromPersistedPolicy(plan, incident,
                new PolicyEvaluation(PolicyDecision.AUTO, List.of(trace), "test"), 10_000, Instant.now());
        action.claim("p1", "c1", "INR", 10_000, "sntl_" + UUID.randomUUID().toString().replace("-", ""),
                Instant.now(), Instant.now().plusSeconds(3600));
        action.complete(linkId, "https://rzp.io/i/test", "created", Instant.now());
        actions.saveAndFlush(action); return incident.getIncidentId();
    }
    private byte[] payload(String linkId, long amount, long paid, String currency) {
        return ("{\"event\":\"payment_link.paid\",\"payload\":{\"payment_link\":{\"entity\":{" +
                "\"id\":\"" + linkId + "\",\"amount\":" + amount + ",\"amount_paid\":" + paid +
                ",\"currency\":\"" + currency + "\",\"status\":\"paid\"}}}}")
                .getBytes(StandardCharsets.UTF_8);
    }
}
