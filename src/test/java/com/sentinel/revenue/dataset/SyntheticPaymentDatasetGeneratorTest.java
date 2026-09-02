package com.sentinel.revenue.dataset;

import com.sentinel.revenue.api.PaymentEventBatchRequest;
import com.sentinel.revenue.api.PaymentEventRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SyntheticPaymentDatasetGeneratorTest {

    @Test
    void generatesRepeatableLabelledDatasetCoveringEveryScenario() {
        PaymentEventBatchRequest first = new SyntheticPaymentDatasetGenerator().generate();
        PaymentEventBatchRequest second = new SyntheticPaymentDatasetGenerator().generate();

        assertThat(first).isEqualTo(second);
        assertThat(first.events()).hasSize(300);
        assertThat(first.events()).allSatisfy(event -> {
            assertThat(event.metadata()).containsEntry("synthetic", true);
            assertThat(event.metadata()).containsKey("groundTruthLabel");
            assertThat(event.customerId()).startsWith("customer_");
        });

        Set<String> labels = first.events().stream()
                .map(PaymentEventRequest::metadata)
                .map(Map::copyOf)
                .map(metadata -> metadata.get("groundTruthLabel").toString())
                .collect(Collectors.toSet());
        assertThat(labels).containsExactlyInAnyOrder(
                "UPI_DEGRADATION", "PROVIDER_OUTAGE", "NORMAL_FAILURE_MIX",
                "INSUFFICIENT_FUNDS", "CUSTOMER_ABANDONMENT",
                "MIXED_METHOD_DEGRADATION", "ALREADY_PAID", "HIGH_VALUE",
                "DUPLICATE", "API_FAILURE");

        long distinctIdempotencyKeys = first.events().stream()
                .map(event -> event.paymentId() + ":" + event.attemptNumber())
                .distinct()
                .count();
        assertThat(distinctIdempotencyKeys).isEqualTo(285);
    }

    @Test
    void namespacesKeepWorkloadCasesDistinct() {
        PaymentEventBatchRequest first = new SyntheticPaymentDatasetGenerator()
                .generateScenario(SyntheticPaymentDatasetGenerator.Scenario.UPI_DEGRADATION, "workload-01");
        PaymentEventBatchRequest second = new SyntheticPaymentDatasetGenerator()
                .generateScenario(SyntheticPaymentDatasetGenerator.Scenario.UPI_DEGRADATION, "workload-02");

        assertThat(first.events()).allSatisfy(event ->
                assertThat(event.metadata()).containsEntry("merchantId", "merchant_workload-01"));
        assertThat(second.events()).allSatisfy(event ->
                assertThat(event.metadata()).containsEntry("merchantId", "merchant_workload-02"));
        assertThat(first.events().stream().map(PaymentEventRequest::paymentId))
                .doesNotContainAnyElementsOf(second.events().stream()
                        .map(PaymentEventRequest::paymentId).toList());
    }

    @Test
    void operationalProfilesRemainDistinctAndDetectionReady() {
        PaymentEventBatchRequest upi = new SyntheticPaymentDatasetGenerator()
                .generateOperationalIncident("workload-01", "UPI", "HDFC",
                        "UPI_ISSUER_UNAVAILABLE", 47_512);
        PaymentEventBatchRequest timeout = new SyntheticPaymentDatasetGenerator()
                .generateOperationalIncident("workload-02", "CARD", "RAZORPAY_GATEWAY",
                        "PAYMENT_TIMED_OUT", 38_900);

        assertThat(upi.events()).hasSize(30).allSatisfy(event -> {
            assertThat(event.status()).isEqualTo("FAILED");
            assertThat(event.metadata()).containsEntry("expectedAnomaly", true);
        });
        assertThat(timeout.events()).allSatisfy(event ->
                assertThat(event.metadata()).containsEntry("merchantId", "merchant_workload-02"));
        assertThat(upi.events().get(0).amountMinor()).isNotEqualTo(timeout.events().get(0).amountMinor());
        assertThat(upi.events().get(0).metadata().get("groundTruthLabel"))
                .isNotEqualTo(timeout.events().get(0).metadata().get("groundTruthLabel"));
    }
}
