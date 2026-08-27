package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.AuditEvent;
import com.sentinel.revenue.model.FindingSource;
import com.sentinel.revenue.model.HistoricalIncident;
import com.sentinel.revenue.model.IncidentFinding;
import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.model.ProcessedWebhookEvent;
import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RecoveryActionStatus;
import com.sentinel.revenue.model.RecoveryOutcome;
import com.sentinel.revenue.model.RecoveryOutcomeStatus;
import com.sentinel.revenue.model.RecoveryPlan;
import com.sentinel.revenue.model.RecoveryStrategy;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.model.RiskLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class RevenueRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-15T09:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired PaymentEventRepository paymentEvents;
    @Autowired RevenueIncidentRepository incidents;
    @Autowired IncidentFindingRepository findings;
    @Autowired RecoveryPlanRepository plans;
    @Autowired RecoveryActionRepository actions;
    @Autowired RecoveryOutcomeRepository outcomes;
    @Autowired AuditEventRepository auditEvents;
    @Autowired ProcessedWebhookEventRepository webhooks;
    @Autowired HistoricalIncidentRepository historicalIncidents;

    @Test
    void paymentEventRepositoryPersistsMinorUnitsAndIdempotencyKey() {
        PaymentEvent event = paymentEvents.saveAndFlush(new PaymentEvent(
                "pay_1", "order_1", "customer_1", 12_345, "INR", "UPI",
                "HDFC", "FAILED", "NETWORK_ERROR", "network error", NOW,
                1, "CARD", 2, null, Map.of("synthetic", true)));

        assertThat(event.getId()).isNotNull();
        assertThat(event.getAmountMinor()).isEqualTo(12_345);
        assertThat(paymentEvents.existsByPaymentIdAndAttemptNumber("pay_1", 1)).isTrue();
    }

    @Test
    void revenueIncidentRepositoryPersistsDurableIncidentState() {
        RevenueIncident incident = persistIncident();

        assertThat(incidents.findById(incident.getIncidentId())).hasValueSatisfying(saved -> {
            assertThat(saved.getStatus()).isEqualTo(RevenueIncidentStatus.DETECTED);
            assertThat(saved.getAmountAtRiskMinor()).isEqualTo(250_000);
            assertThat(saved.getAffectedPayments()).containsExactly("pay_1");
        });
    }

    @Test
    void incidentFindingRepositoryPersistsAgentEvidence() {
        RevenueIncident incident = persistIncident();
        findings.saveAndFlush(new IncidentFinding(
                incident, FindingSource.ROOT_CAUSE_AGENT, "UPI issuer degradation",
                new BigDecimal("0.9100"), List.of("issuer failures concentrated"), NOW));

        assertThat(findings.findAllByIncidentIncidentId(incident.getIncidentId()))
                .singleElement()
                .satisfies(finding -> assertThat(finding.getConfidence())
                        .isEqualByComparingTo("0.9100"));
    }

    @Test
    void recoveryPlanRepositoryPersistsPlanAmountsAsIntegers() {
        RevenueIncident incident = persistIncident();
        RecoveryPlan plan = plans.saveAndFlush(planFor(incident));

        assertThat(plans.findById(plan.getId())).hasValueSatisfying(saved -> {
            assertThat(saved.getStrategy()).isEqualTo(RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK);
            assertThat(saved.getTargetAmountMinor()).isEqualTo(200_000);
        });
    }

    @Test
    void recoveryActionRepositoryPersistsNullablePolicyFields() {
        RevenueIncident incident = persistIncident();
        RecoveryPlan plan = plans.saveAndFlush(planFor(incident));
        RecoveryAction action = actions.saveAndFlush(new RecoveryAction(
                plan, incident, RecoveryActionStatus.PROPOSED, null,
                null, null, 50_000, NOW, null, null));

        assertThat(actions.findAllByRecoveryPlanId(plan.getId()))
                .singleElement()
                .satisfies(saved -> {
                    assertThat(saved.getId()).isEqualTo(action.getId());
                    assertThat(saved.getPolicyDecision()).isNull();
                });
    }

    @Test
    void recoveryOutcomeRepositoryPersistsResolvedAction() {
        RevenueIncident incident = persistIncident();
        RecoveryPlan plan = plans.saveAndFlush(planFor(incident));
        RecoveryAction action = actions.saveAndFlush(new RecoveryAction(
                plan, incident, RecoveryActionStatus.EXECUTED, PolicyDecision.AUTO,
                "payment_link", "plink_1", 50_000, NOW, NOW, NOW));
        RecoveryOutcome outcome = outcomes.saveAndFlush(new RecoveryOutcome(
                action, incident, RecoveryOutcomeStatus.RECOVERED, 50_000,
                NOW, "event_1"));

        assertThat(outcomes.findAllByRecoveryActionId(action.getId()))
                .singleElement()
                .satisfies(saved -> assertThat(saved.getId()).isEqualTo(outcome.getId()));
    }

    @Test
    void auditEventRepositoryOrdersImmutableIncidentHistory() {
        RevenueIncident incident = persistIncident();
        auditEvents.saveAndFlush(new AuditEvent(
                incident, NOW, "SENTINEL", "DETECTOR", "INCIDENT_DETECTED",
                List.of("failure spike"), new BigDecimal("0.9000"),
                "open incident", List.of(), null, null,
                null, RevenueIncidentStatus.DETECTED, "created"));

        assertThat(auditEvents.findAllByIncidentIncidentIdOrderByTimestampAsc(
                incident.getIncidentId()))
                .singleElement()
                .satisfies(event -> assertThat(event.getAction()).isEqualTo("INCIDENT_DETECTED"));
    }

    @Test
    void processedWebhookRepositoryEnforcesExternalEventIdentity() {
        ProcessedWebhookEvent webhook = webhooks.saveAndFlush(new ProcessedWebhookEvent(
                "event_1", "payment_link.paid", NOW, null,
                Map.of("event", "payment_link.paid"), true));

        assertThat(webhooks.existsByEventId("event_1")).isTrue();
        assertThat(webhooks.findByEventId("event_1"))
                .hasValueSatisfying(saved -> assertThat(saved.getId()).isEqualTo(webhook.getId()));
    }

    @Test
    void historicalIncidentRepositoryPersistsReusableOutcomeWithoutAggregateRate() {
        RevenueIncident incident = persistIncident();
        HistoricalIncident historical = historicalIncidents.saveAndFlush(new HistoricalIncident(
                incident, "issuer degradation", Map.of("issuer", "HDFC"),
                RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK,
                RecoveryOutcomeStatus.RECOVERED, 75_000, NOW));

        assertThat(historicalIncidents.findById(historical.getId()))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getOriginalIncidentId()).isEqualTo(incident.getIncidentId());
                    assertThat(saved.getRecoveredAmountMinor()).isEqualTo(75_000);
                });
    }

    private RevenueIncident persistIncident() {
        return incidents.saveAndFlush(new RevenueIncident(
                "PAYMENT_DEGRADATION", RevenueIncidentStatus.DETECTED, "HIGH",
                250_000, NOW, List.of("pay_1"), List.of("customer_1"),
                List.of("success rate below baseline"), null, null));
    }

    private RecoveryPlan planFor(RevenueIncident incident) {
        return new RecoveryPlan(
                incident, RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK,
                "UPI is degraded", 4, 200_000, new BigDecimal("0.8700"),
                150_000, RiskLevel.LOW, NOW);
    }
}
