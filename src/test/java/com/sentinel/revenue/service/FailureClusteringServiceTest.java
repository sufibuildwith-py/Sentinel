package com.sentinel.revenue.service;

import com.sentinel.revenue.detection.DetectionProperties;
import com.sentinel.revenue.detection.DetectionRuleEngine;
import com.sentinel.revenue.detection.FailureCluster;
import com.sentinel.revenue.detection.StatisticsEngine;
import com.sentinel.revenue.api.PaymentEventRequest;
import com.sentinel.revenue.dataset.SyntheticPaymentDatasetGenerator;
import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.repository.IncidentFindingRepository;
import com.sentinel.revenue.repository.PaymentEventRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FailureClusteringServiceTest {

    private final DetectionProperties properties = new DetectionProperties(
            Duration.ofHours(1), Duration.ofHours(1), Duration.ofHours(24),
            Duration.ofMinutes(15), 10, 0.20, 2.0, 100_000,
            0.95, 0.05, Set.of("CAPTURED", "AUTHORIZED"), "merchantId");

    private final FailureClusteringService service = new FailureClusteringService(
            mock(PaymentEventRepository.class),
            mock(RevenueIncidentRepository.class),
            mock(IncidentFindingRepository.class),
            new StatisticsEngine(properties),
            new DetectionRuleEngine(properties),
            properties);

    @Test
    void groupsFailuresByFullSignatureAndStartsNewTimeWindows() {
        Instant start = Instant.parse("2026-01-15T09:00:00Z");
        List<FailureCluster> clusters = service.clusterFailures(List.of(
                event("pay_1", "UPI", "HDFC", "ISSUER_DOWN", "merchant_a", start),
                event("pay_2", "UPI", "HDFC", "ISSUER_DOWN", "merchant_a",
                        start.plus(Duration.ofMinutes(30))),
                event("pay_3", "UPI", "HDFC", "ISSUER_DOWN", "merchant_a",
                        start.plus(Duration.ofMinutes(61))),
                event("pay_4", "UPI", "HDFC", "ISSUER_DOWN", "merchant_b", start),
                event("pay_5", "CARD", "HDFC", "ISSUER_DOWN", "merchant_a", start),
                event("pay_success", "UPI", "HDFC", null, "merchant_a", start)));

        assertThat(clusters).hasSize(4);
        assertThat(clusters)
                .filteredOn(cluster -> cluster.key().merchantId().equals("MERCHANT_A")
                        && cluster.key().method().equals("UPI"))
                .extracting(cluster -> cluster.contributingEvents().size())
                .containsExactly(2, 1);
        assertThat(clusters).allSatisfy(cluster ->
                assertThat(cluster.contributingEvents())
                        .allSatisfy(event -> assertThat(event.getStatus()).isEqualTo("FAILED")));
    }

    @Test
    void labelledDatasetHasOnlyUpiAndProviderClustersAboveMinimumVolume() {
        List<PaymentEvent> events = new SyntheticPaymentDatasetGenerator().generate().events()
                .stream()
                .map(this::entity)
                .toList();

        assertThat(service.clusterFailures(events))
                .filteredOn(cluster -> cluster.contributingEvents().size()
                        >= properties.minimumVolume())
                .extracting(cluster -> cluster.key().errorCode())
                .containsExactlyInAnyOrder(
                        "UPI_ISSUER_UNAVAILABLE", "PROVIDER_UNAVAILABLE");
    }

    private PaymentEvent event(String paymentId, String method, String issuer,
                               String errorCode, String merchant, Instant timestamp) {
        String status = errorCode == null ? "CAPTURED" : "FAILED";
        return new PaymentEvent(paymentId, "order_" + paymentId,
                "customer_" + paymentId, 20_000, "INR", method, issuer,
                status, errorCode, errorCode, timestamp, 1, null, 0, null,
                Map.of("merchantId", merchant));
    }

    private PaymentEvent entity(PaymentEventRequest request) {
        return new PaymentEvent(
                request.paymentId(), request.orderId(), request.customerId(),
                request.amountMinor(), request.currency(), request.method(),
                request.issuerBank(), request.status(), request.errorCode(),
                request.errorDescription(), request.timestamp(), request.attemptNumber(),
                request.previousSuccessfulMethod(), request.previousFailureCount(),
                request.subscriptionId(), request.metadata());
    }
}
