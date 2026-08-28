package com.sentinel.revenue.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("razorpay-smoke")
@EnabledIfEnvironmentVariable(named = "RAZORPAY_SMOKE", matches = "true")
class RazorpayTestModeSmokeTest {
    @Test void createsOneRealTestModeLink() {
        RazorpayProperties properties = new RazorpayProperties(true,
                System.getenv("RAZORPAY_KEY_ID"), System.getenv("RAZORPAY_KEY_SECRET"),
                URI.create("https://api.razorpay.com"), Duration.ofSeconds(2), Duration.ofSeconds(5),
                Duration.ofMinutes(30), Duration.ofHours(1), 2, 50, 2, 4,
                Duration.ofSeconds(30), false);
        RazorpayGateway gateway = new RazorpayHttpGateway(HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout()).build(), new ObjectMapper(), properties);
        String reference = "sntl_" + UUID.randomUUID().toString().replace("-", "");
        PaymentLinkResource link = gateway.createPaymentLink(new PaymentLinkCommand(100, "INR", reference,
                "Sentinel Test Mode smoke check", Instant.now().plusSeconds(1800), UUID.randomUUID(),
                "ref_smoke", true, false));
        assertThat(link.id()).startsWith("plink_");
        assertThat(link.referenceId()).isEqualTo(reference);
    }
}
