package com.sentinel.revenue.detection;

import com.sentinel.revenue.model.PaymentEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StatisticsEngineTest {

    private final StatisticsEngine engine = new StatisticsEngine(properties());

    @Test
    void computesAllRequiredWindowAndRollingBaselineMetrics() {
        Instant windowStart = Instant.parse("2026-01-15T10:00:00Z");
        List<PaymentEvent> window = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            window.add(event("captured_" + index, 10_000 + index, "UPI", "HDFC",
                    "CAPTURED", null, windowStart.plusSeconds(index * 60L), 0));
        }
        window.add(event("failed_1", 20_000, "UPI", "HDFC", "FAILED",
                "NETWORK_ERROR", windowStart.plusSeconds(420), 1));
        window.add(event("failed_2", 30_000, "UPI", "HDFC", "FAILED",
                "NETWORK_ERROR", windowStart.plusSeconds(480), 1));
        window.add(event("failed_3", 40_000, "CARD", "ICICI", "FAILED",
                "ISSUER_ERROR", windowStart.plusSeconds(540), 1));
        window.add(event("abandoned", 15_000, "CARD", "ICICI", "ABANDONED",
                null, windowStart.plusSeconds(600), 0));

        List<PaymentEvent> baseline = baselineAt(
                Instant.parse("2026-01-15T09:00:00Z"));
        PaymentStatistics statistics = engine.compute(window, baseline);

        assertThat(statistics.eventCount()).isEqualTo(10);
        assertThat(statistics.failedEventCount()).isEqualTo(3);
        assertThat(statistics.overallSuccessRate()).isCloseTo(0.6, within(0.0001));
        assertThat(statistics.successRateByMethod()).containsEntry("UPI", 6.0 / 8.0)
                .containsEntry("CARD", 0.0);
        assertThat(statistics.successRateByIssuer()).containsEntry("HDFC", 6.0 / 8.0)
                .containsEntry("ICICI", 0.0);
        assertThat(statistics.failureCodeDistribution())
                .containsEntry("NETWORK_ERROR", 2L)
                .containsEntry("ISSUER_ERROR", 1L);
        assertThat(statistics.retryFrequency()).isCloseTo(0.3, within(0.0001));
        assertThat(statistics.abandonmentRate()).isCloseTo(0.1, within(0.0001));
        assertThat(statistics.amountAtRiskMinor()).isEqualTo(90_000);
        assertThat(statistics.valueConcentration()).isGreaterThan(0.0);
        assertThat(statistics.baselineSuccessRate()).isCloseTo(0.9, within(0.0001));
        assertThat(statistics.baselineStandardDeviation()).isEqualTo(0.05);
        assertThat(statistics.baselineDeviation()).isCloseTo(6.0, within(0.0001));
        assertThat(statistics.baselineBucketCount()).isEqualTo(2);
    }

    private List<PaymentEvent> baselineAt(Instant start) {
        List<PaymentEvent> events = new ArrayList<>();
        for (int bucket = 0; bucket < 2; bucket++) {
            Instant bucketStart = start.plus(Duration.ofMinutes(15L * bucket));
            for (int index = 0; index < 10; index++) {
                String status = index == 9 ? "FAILED" : "CAPTURED";
                events.add(event("baseline_" + bucket + "_" + index, 10_000,
                        "UPI", "HDFC", status,
                        index == 9 ? "NETWORK_ERROR" : null,
                        bucketStart.plusSeconds(index * 30L), 0));
            }
        }
        return events;
    }

    private PaymentEvent event(String paymentId, long amountMinor, String method,
                               String issuer, String status, String errorCode,
                               Instant timestamp, int previousFailures) {
        return new PaymentEvent(paymentId, "order_" + paymentId,
                "customer_" + paymentId, amountMinor, "INR", method, issuer,
                status, errorCode, errorCode, timestamp, 1, null,
                previousFailures, null, Map.of("merchantId", "merchant_demo"));
    }

    private DetectionProperties properties() {
        return new DetectionProperties(
                Duration.ofHours(1), Duration.ofHours(1), Duration.ofHours(24),
                Duration.ofMinutes(15), 10, 0.20, 2.0, 100_000,
                0.95, 0.05, Set.of("CAPTURED", "AUTHORIZED"), "merchantId");
    }
}
