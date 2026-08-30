package com.sentinel.revenue.service;

import com.sentinel.revenue.model.ProviderOrder;
import com.sentinel.revenue.model.RecoveryJob;
import com.sentinel.revenue.model.WebhookEvent;
import com.sentinel.revenue.repository.PaymentDowntimeRepository;
import com.sentinel.revenue.repository.ProviderOrderRepository;
import com.sentinel.revenue.repository.ProviderPaymentRepository;
import com.sentinel.revenue.repository.RecoveryJobRepository;
import com.sentinel.revenue.repository.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({RecoveryJobService.class, WebhookEventService.class})
class LiveProviderPersistenceIntegrationTest {

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

    @Autowired ProviderOrderRepository providerOrders;
    @Autowired ProviderPaymentRepository providerPayments;
    @Autowired PaymentDowntimeRepository paymentDowntimes;
    @Autowired WebhookEventRepository webhookEvents;
    @Autowired RecoveryJobRepository recoveryJobs;
    @Autowired RecoveryJobService recoveryJobService;
    @Autowired WebhookEventService webhookEventService;

    @Test
    void providerRepositoriesSupportIdempotentOrderLookupAndAllNewTables() {
        UUID incidentId = UUID.randomUUID();
        ProviderOrder order = providerOrders.saveAndFlush(new ProviderOrder(
                incidentId, "order_test_1", 12_345, "INR", "CREATED",
                "incident-ref", "incident-1-recovery-1"));

        assertThat(providerOrders.findByIdempotencyKey("incident-1-recovery-1"))
                .hasValueSatisfying(saved -> assertThat(saved.getId()).isEqualTo(order.getId()));
        assertThat(providerOrders.findByRazorpayOrderId("order_test_1")).isPresent();

        providerPayments.saveAndFlush(new com.sentinel.revenue.model.ProviderPayment(
                order.getId(), "pay_test_1", "order_test_1", "captured", 12_345L,
                "upi", NOW, "{\"id\":\"pay_test_1\"}"));
        paymentDowntimes.saveAndFlush(new com.sentinel.revenue.model.PaymentDowntime(
                "downtime_1", "upi", "issuer", NOW.minusSeconds(60), NOW,
                "RESOLVED", "{\"method\":\"upi\"}"));

        assertThat(providerPayments.findByRazorpayPaymentId("pay_test_1")).isPresent();
        assertThat(paymentDowntimes.count()).isEqualTo(1);
        assertThatThrownBy(() -> providerOrders.saveAndFlush(new ProviderOrder(
                incidentId, "order_test_2", 12_345, "INR", "CREATED",
                "incident-ref", "incident-1-recovery-1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void webhookEventIdIsUniqueAndServiceMarksEventsProcessed() {
        WebhookEvent event = webhookEventService.persist(
                "evt_test_1", "payment.captured", "{\"id\":\"evt_test_1\"}", "sig");

        assertThat(webhookEventService.isDuplicate("evt_test_1")).isTrue();
        assertThat(webhookEventService.findUnprocessed()).extracting(WebhookEvent::getEventId)
                .containsExactly("evt_test_1");

        WebhookEvent processed = webhookEventService.markProcessed(event.getId(), UUID.randomUUID());
        assertThat(processed.isProcessed()).isTrue();
        assertThat(webhookEventService.findUnprocessed()).isEmpty();
        assertThatThrownBy(() -> webhookEventService.persist(
                "evt_test_1", "payment.captured", "{}", "sig"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicateWebhookEventIsRejectedByDatabaseConstraint() {
        webhookEvents.saveAndFlush(new WebhookEvent(
                "evt_unique_1", "payment.captured", "{}", "sig",
                false, false, null, NOW, null));

        assertThatThrownBy(() -> webhookEvents.saveAndFlush(new WebhookEvent(
                "evt_unique_1", "payment.captured", "{}", "sig",
                false, false, null, NOW, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void recoveryJobTransitionsAndExhaustsAfterMaximumAttempts() {
        RecoveryJob job = recoveryJobService.createJob(
                UUID.randomUUID(), UUID.randomUUID(), "DEFERRED_RETRY");

        assertThat(recoveryJobService.markRunning(job.getId()).getStatus())
                .isEqualTo(RecoveryJob.RUNNING);
        assertThat(recoveryJobService.markSucceeded(job.getId()).getStatus())
                .isEqualTo(RecoveryJob.SUCCEEDED);

        RecoveryJob retrying = recoveryJobService.createJob(
                UUID.randomUUID(), null, "RECOVERY_REMINDER");
        for (int attempt = 0; attempt < retrying.getMaxAttempts(); attempt++) {
            recoveryJobService.markRunning(retrying.getId());
            recoveryJobService.markFailed(retrying.getId(), "provider unavailable");
        }
        assertThat(recoveryJobs.findById(retrying.getId()).orElseThrow().getStatus())
                .isEqualTo(RecoveryJob.EXHAUSTED);
        assertThat(recoveryJobs.findById(retrying.getId()).orElseThrow().getAttemptCount())
                .isEqualTo(3);
    }

    @Test
    void findPendingDueJobsExcludesFutureAndNonPendingJobs() {
        RecoveryJob due = recoveryJobs.saveAndFlush(new RecoveryJob(
                UUID.randomUUID(), null, "WAIT_FOR_PROVIDER", 3,
                Instant.now().minusSeconds(30)));
        RecoveryJob future = recoveryJobs.saveAndFlush(new RecoveryJob(
                UUID.randomUUID(), null, "WAIT_FOR_PROVIDER", 3,
                Instant.now().plusSeconds(3_600)));
        RecoveryJob succeeded = recoveryJobs.saveAndFlush(new RecoveryJob(
                UUID.randomUUID(), null, "WAIT_FOR_PROVIDER", 3,
                Instant.now().minusSeconds(30)));
        recoveryJobService.markRunning(succeeded.getId());
        recoveryJobService.markSucceeded(succeeded.getId());

        assertThat(recoveryJobService.findPendingDueJobs())
                .extracting(RecoveryJob::getId)
                .containsExactly(due.getId())
                .doesNotContain(future.getId(), succeeded.getId());
    }
}
