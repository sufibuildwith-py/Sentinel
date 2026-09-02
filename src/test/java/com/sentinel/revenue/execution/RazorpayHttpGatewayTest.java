package com.sentinel.revenue.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

class RazorpayHttpGatewayTest {
    private WireMockServer server;

    @BeforeEach void start() { server = new WireMockServer(0); server.start(); }
    @AfterEach void stop() { server.stop(); }

    @Test void createsStandardLinkWithExactAmountUpiDisabledAndNoSecretsInPayload() {
        server.stubFor(post(urlEqualTo("/v1/payment_links")).willReturn(okJson("""
                {"id":"plink_1","reference_id":"sntl_123","short_url":"https://rzp.io/i/abc","status":"created"}
                """)));
        RazorpayHttpGateway gateway = gateway(3, Duration.ofSeconds(2), 3);
        PaymentLinkCommand command = new PaymentLinkCommand(12_345, "INR", "sntl_123",
                "Sentinel recovery", Instant.now().plusSeconds(3600), UUID.randomUUID(),
                "ref_a1b2c3d4", true, false);

        assertThat(gateway.createPaymentLink(command).id()).isEqualTo("plink_1");
        server.verify(1, postRequestedFor(urlEqualTo("/v1/payment_links"))
                .withRequestBody(matchingJsonPath("$.amount", equalTo("12345")))
                .withRequestBody(matchingJsonPath("$.accept_partial", equalTo("false")))
                .withRequestBody(matchingJsonPath("$.options.checkout.method.upi", equalTo("0")))
                .withRequestBody(matchingJsonPath("$.options.checkout.method.card", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.options.checkout.method.netbanking", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.expire_by", matching("[0-9]+")))
                .withRequestBody(notMatching(".*secret-value.*")));
    }

    @Test void structured400IsSanitizedAndNonRetryable() {
        server.stubFor(post(urlEqualTo("/v1/payment_links")).willReturn(aResponse().withStatus(400)
                .withBody("""
                        {"error":{"code":"BAD_REQUEST_ERROR","field":"expire_by",
                        "description":"expire_by must be within six months","source":"business",
                        "step":"payment_initiation","reason":"invalid_expiry","secret":"do-not-leak"}}
                        """)));

        RazorpayFailure failure = catchThrowableOfType(
                () -> gateway(3, Duration.ofSeconds(1), 3).createPaymentLink(command()), RazorpayFailure.class);

        assertThat(failure.kind()).isEqualTo(RazorpayFailure.Kind.NON_RETRYABLE);
        assertThat(failure.safeCode()).isEqualTo("BAD_REQUEST_ERROR");
        assertThat(failure.providerError()).satisfies(error -> {
            assertThat(error.httpStatus()).isEqualTo(400);
            assertThat(error.field()).isEqualTo("expire_by");
            assertThat(error.description()).contains("six months");
            assertThat(error.safeSummary()).doesNotContain("do-not-leak");
        });
        assertThat(failure.getMessage()).doesNotContain("do-not-leak");
        server.verify(1, postRequestedFor(urlEqualTo("/v1/payment_links")));
    }

    @Test void safeReadRetries429ButCreate400IsNotRetriedAndErrorsAreSanitized() {
        server.stubFor(get(urlPathEqualTo("/v1/payment_links")).willReturn(aResponse().withStatus(429)));
        RazorpayHttpGateway gateway = gateway(2, Duration.ofSeconds(1), 3);
        assertThatThrownBy(() -> gateway.findPaymentLinkByReference("sntl_123"))
                .isInstanceOf(RazorpayFailure.class).hasMessageContaining("HTTP_429")
                .hasMessageNotContaining("secret-value");
        server.verify(2, getRequestedFor(urlPathEqualTo("/v1/payment_links")));

        server.resetAll();
        server.stubFor(post(urlEqualTo("/v1/payment_links")).willReturn(aResponse().withStatus(400)
                .withBody("{\"error\":\"secret-value should never escape\"}")));
        assertThatThrownBy(() -> gateway.createPaymentLink(command()))
                .isInstanceOf(RazorpayFailure.class).hasMessage("Razorpay request failed (HTTP_400)")
                .hasMessageNotContaining("secret-value should never escape");
        server.verify(1, postRequestedFor(urlEqualTo("/v1/payment_links")));
    }

    @Test void timeoutAndMalformedResponseAreTypedWithoutLeakingCredentials() {
        server.stubFor(post(urlEqualTo("/v1/payment_links")).willReturn(okJson("not-json")));
        assertThatThrownBy(() -> gateway(1, Duration.ofSeconds(1), 3).createPaymentLink(command()))
                .isInstanceOf(RazorpayFailure.class).hasMessageContaining("MALFORMED_RESPONSE");

        server.resetAll();
        server.stubFor(post(urlEqualTo("/v1/payment_links")).willReturn(okJson("{}")
                .withFixedDelay(200)));
        assertThatThrownBy(() -> gateway(1, Duration.ofMillis(30), 3).createPaymentLink(command()))
                .isInstanceOf(RazorpayFailure.class).hasMessageContaining("TIMEOUT")
                .hasMessageNotContaining("secret-value");
    }

    @Test void repeatedTemporaryFailuresOpenCircuit() {
        server.stubFor(get(urlEqualTo("/v1/payment_links/plink_1")).willReturn(serverError()));
        RazorpayHttpGateway gateway = gateway(1, Duration.ofSeconds(1), 2);
        assertThatThrownBy(() -> gateway.fetchPaymentLink("plink_1")).isInstanceOf(RazorpayFailure.class);
        assertThatThrownBy(() -> gateway.fetchPaymentLink("plink_1")).isInstanceOf(RazorpayFailure.class);
        assertThatThrownBy(() -> gateway.fetchPaymentLink("plink_1"))
                .isInstanceOfSatisfying(RazorpayFailure.class,
                        failure -> assertThat(failure.kind()).isEqualTo(RazorpayFailure.Kind.CIRCUIT_OPEN));
        server.verify(2, getRequestedFor(urlEqualTo("/v1/payment_links/plink_1")));
    }

    @Test void authenticationFailureIsNonRetryableAndSanitized() {
        server.stubFor(post(urlEqualTo("/v1/payment_links")).willReturn(aResponse().withStatus(401)
                .withBody("credential rejected: secret-value")));
        assertThatThrownBy(() -> gateway(3, Duration.ofSeconds(1), 3).createPaymentLink(command()))
                .isInstanceOf(RazorpayFailure.class).hasMessage("Razorpay request failed (HTTP_401)")
                .hasMessageNotContaining("secret-value");
        server.verify(1, postRequestedFor(urlEqualTo("/v1/payment_links")));
    }

    private RazorpayHttpGateway gateway(int attempts, Duration requestTimeout, int minimumCalls) {
        RazorpayProperties properties = new RazorpayProperties(true, "rzp_test_key", "secret-value",
                URI.create(server.baseUrl()), Duration.ofSeconds(1), requestTimeout,
                Duration.ofMinutes(30), Duration.ofHours(24), attempts, 50, minimumCalls,
                minimumCalls, Duration.ofSeconds(30), false);
        return new RazorpayHttpGateway(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                new ObjectMapper(), properties);
    }
    private PaymentLinkCommand command() {
        return new PaymentLinkCommand(100, "INR", "sntl_123", "Sentinel recovery",
                Instant.now().plusSeconds(3600), UUID.randomUUID(), "ref_abcd", true, false);
    }
}
