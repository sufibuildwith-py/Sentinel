package com.sentinel.revenue.communication;

import com.sentinel.revenue.model.PromiseToPay;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class PromiseToPayTest {
    @Test
    void onlyProviderConfirmedUniqueEventsFulfilPromiseMonotonically() {
        Instant now = Instant.parse("2026-08-31T10:00:00Z");
        PromiseToPay promise = new PromiseToPay(UUID.randomUUID(), UUID.randomUUID(), "customer_0182",
                10_000, 10_000, now.plusSeconds(86400), now);
        assertThat(promise.applyConfirmedPayment(4_000, "evt-1", now.plusSeconds(60))).isTrue();
        assertThat(promise.getStatus()).isEqualTo(PromiseStatus.PARTIALLY_KEPT);
        assertThat(promise.applyConfirmedPayment(4_000, "evt-1", now.plusSeconds(120))).isFalse();
        assertThat(promise.applyConfirmedPayment(6_000, "evt-2", now.plusSeconds(180))).isTrue();
        assertThat(promise.getStatus()).isEqualTo(PromiseStatus.KEPT);
        assertThat(promise.getFulfilledAmountMinor()).isEqualTo(10_000);
    }
}
