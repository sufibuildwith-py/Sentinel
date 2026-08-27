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
}
