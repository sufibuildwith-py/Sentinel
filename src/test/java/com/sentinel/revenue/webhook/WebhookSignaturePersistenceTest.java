package com.sentinel.revenue.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.revenue.repository.WebhookEventRepository;
import com.sentinel.revenue.service.WebhookEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class WebhookSignaturePersistenceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    WebhookEventRepository repository;

    @Test
    void validSignaturePersistsOnceAndDuplicateIsAcknowledged() throws Exception {
        String secret = "webhook-test-secret";
        byte[] body = "{\"event\":\"payment.authorized\",\"payload\":{}}"
                .getBytes(StandardCharsets.UTF_8);
        String signature = WebhookSignatureVerifierTest.sign(body, secret);
        WebhookOutcomeProcessor processor = mock(WebhookOutcomeProcessor.class);
        when(processor.process(anyString(), any(), anyString()))
                .thenReturn(new WebhookResult("evt_live_1", "IGNORED", false, "safe"));
        when(processor.duplicate("evt_live_1"))
                .thenReturn(new WebhookResult("evt_live_1", "DUPLICATE", true, "safe"));
        WebhookEventService events = new WebhookEventService(repository);
        WebhookRequestHandler handler = new WebhookRequestHandler(
                new WebhookSignatureVerifier(new RazorpayWebhookProperties(secret)),
                mock(WebhookSecurityAuditService.class), processor,
                new ObjectMapper(), events, mock(RecoveryJobEnqueuer.class));

        WebhookResult first = handler.handle(body, signature, "evt_live_1");
        WebhookResult duplicate = handler.handle(body, signature, "evt_live_1");

        assertThat(first.duplicate()).isFalse();
        assertThat(duplicate.duplicate()).isTrue();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findByEventId("evt_live_1")).get()
                .satisfies(event -> {
                    assertThat(event.isVerified()).isTrue();
                    assertThat(event.isProcessed()).isTrue();
                });
    }

    @Test
    void invalidSignatureIsRejectedBeforePersistenceOrParsing() {
        byte[] body = "not-json private@example.com".getBytes(StandardCharsets.UTF_8);
        WebhookOutcomeProcessor processor = mock(WebhookOutcomeProcessor.class);
        WebhookRequestHandler handler = new WebhookRequestHandler(
                new WebhookSignatureVerifier(new RazorpayWebhookProperties("secret")),
                mock(WebhookSecurityAuditService.class), processor,
                new ObjectMapper(), new WebhookEventService(repository), null);

        assertThatThrownBy(() -> handler.handle(body, "invalid", "evt_bad"))
                .isInstanceOf(InvalidWebhookSignatureBadRequestException.class);
        assertThat(repository.count()).isZero();
        verify(processor, never()).process(anyString(), any(), anyString());
    }
}
