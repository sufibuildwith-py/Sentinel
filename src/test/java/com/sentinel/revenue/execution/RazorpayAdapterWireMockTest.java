package com.sentinel.revenue.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.razorpay.RazorpayClient;
import com.sentinel.revenue.model.ProviderOrder;
import com.sentinel.revenue.repository.ProviderOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RazorpayAdapterWireMockTest {

    private WireMockServer server;

    @BeforeEach
    void start() {
        server = new WireMockServer(0);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    @Test
    void paymentLinkCreationIsProviderAndDatabaseIdempotent() {
        server.stubFor(post(urlEqualTo("/v1/payment_links")).willReturn(okJson("""
                {"id":"plink_live_1","reference_id":"sntl_ref","short_url":"https://rzp.io/i/test","status":"created"}
                """)));
        ProviderOrderRepository repository = mock(ProviderOrderRepository.class);
        AtomicReference<ProviderOrder> stored = new AtomicReference<>();
        when(repository.findByIdempotencyKey("recovery-1"))
                .thenAnswer(ignored -> Optional.ofNullable(stored.get()));
        when(repository.saveAndFlush(any(ProviderOrder.class))).thenAnswer(invocation -> {
            ProviderOrder value = invocation.getArgument(0);
            stored.set(value);
            return value;
        });
        RazorpayAdapter adapter = adapter(repository);
        UUID incidentId = UUID.randomUUID();

        assertThat(adapter.createPaymentLink(incidentId, 4_299,
                "Recovery", "recovery-1").getString("id")).isEqualTo("plink_live_1");
        assertThat(adapter.createPaymentLink(incidentId, 4_299,
                "Recovery", "recovery-1").getString("id")).isEqualTo("plink_live_1");

        server.verify(1, postRequestedFor(urlEqualTo("/v1/payment_links"))
                .withRequestBody(matchingJsonPath("$.amount", equalTo("4299")))
                .withRequestBody(matchingJsonPath("$.expire_by", matching("[0-9]+")))
                .withRequestBody(matchingJsonPath("$.notes.sentinel_incident",
                        equalTo(incidentId.toString()))));
        verify(repository).saveAndFlush(any(ProviderOrder.class));
    }

    @Test
    void providerFailureIsTypedAndDoesNotPersist() {
        server.stubFor(post(urlEqualTo("/v1/payment_links"))
                .willReturn(aResponse().withStatus(500).withBody("provider secret must not escape")));
        ProviderOrderRepository repository = mock(ProviderOrderRepository.class);
        when(repository.findByIdempotencyKey("recovery-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter(repository).createPaymentLink(UUID.randomUUID(),
                100, "Recovery", "recovery-2"))
                .isInstanceOf(RazorpayFailure.class)
                .hasMessage("Razorpay request failed (HTTP_500)")
                .hasMessageNotContaining("provider secret");
        verify(repository, org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @SuppressWarnings("unchecked")
    private RazorpayAdapter adapter(ProviderOrderRepository repository) {
        ObjectProvider<RazorpayClient> clients = mock(ObjectProvider.class);
        when(clients.getIfAvailable()).thenReturn(null);
        RazorpayProperties properties = new RazorpayProperties(true, "rzp_test_key", "secret-value",
                URI.create(server.baseUrl()), Duration.ofSeconds(1), Duration.ofSeconds(2),
                Duration.ofMinutes(30), Duration.ofHours(24), 3, 50, 3,
                3, Duration.ofSeconds(30), false);
        return new RazorpayAdapter(clients, properties, repository,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                new ObjectMapper());
    }
}
