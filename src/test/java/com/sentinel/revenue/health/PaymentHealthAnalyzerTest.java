package com.sentinel.revenue.health;

import com.sentinel.revenue.detection.DetectionProperties;
import com.sentinel.revenue.detection.StatisticsEngine;
import com.sentinel.revenue.model.PaymentDowntime;
import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.model.RecoveryJob;
import com.sentinel.revenue.repository.PaymentDowntimeRepository;
import com.sentinel.revenue.repository.PaymentEventRepository;
import com.sentinel.revenue.repository.RecoveryJobRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentHealthAnalyzerTest {
    @Test
    void detectsExplainableMerchantLevelDegradationAcrossIndependentSignals() {
        PaymentEventRepository payments = mock(PaymentEventRepository.class);
        PaymentDowntimeRepository downtimes = mock(PaymentDowntimeRepository.class);
        RecoveryJobRepository jobs = mock(RecoveryJobRepository.class);
        Instant now = Instant.parse("2026-08-31T10:00:00Z");
        List<PaymentEvent> events = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            events.add(event("historic-" + index, "CAPTURED", null, now.minus(Duration.ofHours(2))));
        }
        for (int index = 0; index < 20; index++) {
            events.add(event("failed-" + index, "FAILED", "GATEWAY_TIMEOUT", now.minusSeconds(index * 20L)));
        }
        when(payments.findAllByTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(any(), any()))
                .thenReturn(events);
        when(downtimes.findAll()).thenReturn(List.of(new PaymentDowntime("down-1", "UPI", "HDFC",
                now.minusSeconds(60), null, "ACTIVE", "{}")));
        when(jobs.findAll()).thenReturn(List.of());
        DetectionProperties detection = new DetectionProperties(Duration.ofHours(1), Duration.ofHours(1),
                Duration.ofHours(24), Duration.ofMinutes(15), 10, 0.2, 2, 100000,
                0.95, 0.05, Set.of("CAPTURED", "AUTHORIZED"), "merchantId");
        PaymentHealthAnalyzer analyzer = new PaymentHealthAnalyzer(payments, downtimes, jobs,
                new StatisticsEngine(detection), new PaymentHealthProperties(10, 0.2, 0.2,
                2, 0.6, 100000, 0.3));

        PaymentHealthReport report = analyzer.analyze("merchant-1", now);

        assertThat(report.current()).containsKeys("5m", "15m", "60m");
        assertThat(report.baseline()).containsKeys("24h", "7d");
        assertThat(report.signals()).filteredOn(PaymentHealthSignal::active)
                .extracting(PaymentHealthSignal::type)
                .contains(PaymentHealthSignalType.PAYMENT_METHOD_DEGRADATION,
                        PaymentHealthSignalType.FAILURE_RATE_SPIKE,
                        PaymentHealthSignalType.BANK_CONCENTRATION,
                        PaymentHealthSignalType.ERROR_REASON_CONCENTRATION,
                        PaymentHealthSignalType.REVENUE_EXPOSURE_SPIKE,
                        PaymentHealthSignalType.PROVIDER_DOWNTIME_SIGNAL);
        assertThat(report.signals()).allSatisfy(signal -> assertThat(signal.evidence()).isNotEmpty());
    }

    private PaymentEvent event(String id, String status, String error, Instant timestamp) {
        return new PaymentEvent(id, "order-" + id, "customer-" + id, 10000, "INR",
                "UPI", "HDFC", status, error, error, timestamp, 1,
                null, 0, null, Map.of("merchantId", "merchant-1"));
    }
}
