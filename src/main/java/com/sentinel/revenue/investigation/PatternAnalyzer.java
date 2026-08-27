package com.sentinel.revenue.investigation;

import com.sentinel.revenue.detection.DetectionProperties;
import com.sentinel.revenue.detection.PaymentStatistics;
import com.sentinel.revenue.detection.StatisticsEngine;
import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.repository.PaymentEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PatternAnalyzer {
    private final PaymentEventRepository payments;
    private final StatisticsEngine statisticsEngine;
    private final DetectionProperties properties;

    public PatternAnalyzer(PaymentEventRepository payments, StatisticsEngine statisticsEngine,
                           DetectionProperties properties) {
        this.payments = payments;
        this.statisticsEngine = statisticsEngine;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public PatternAnalysis analyze(RevenueIncident incident) {
        List<PaymentEvent> affected = payments.findAllByPaymentIdIn(incident.getAffectedPayments());
        Instant start = affected.stream().map(PaymentEvent::getTimestamp)
                .min(Comparator.naturalOrder()).orElse(incident.getDetectedAt());
        List<PaymentEvent> baseline = payments
                .findAllByTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(
                        start.minus(properties.baselineWindow()), start);
        PaymentStatistics statistics = statisticsEngine.compute(affected, baseline);

        List<PaymentEvent> failures = affected.stream().filter(this::failed).toList();
        List<String> evidence = new ArrayList<>();
        evidence.add(rateLine("failures", failures.size(), affected.size()));
        addDominant(evidence, "payment method", failures, PaymentEvent::getMethod);
        addDominant(evidence, "issuer/bank", failures, PaymentEvent::getIssuerBank);
        addDominant(evidence, "failure code", failures, PaymentEvent::getErrorCode);
        evidence.add("Success rate %s versus rolling baseline %s: drop %s and deviation %s standard deviations."
                .formatted(percent(statistics.overallSuccessRate()), percent(statistics.baselineSuccessRate()),
                        percent(statistics.successRateDrop()), decimal(statistics.baselineDeviation())));
        evidence.add("Retry frequency is %s (%d-event cohort); abandonment rate is %s."
                .formatted(percent(statistics.retryFrequency()), statistics.eventCount(),
                        percent(statistics.abandonmentRate())));

        double dominantShare = dominantShare(failures, PaymentEvent::getErrorCode);
        return new PatternAnalysis(statistics, evidence, dominantShare);
    }

    private void addDominant(List<String> evidence, String label, List<PaymentEvent> events,
                             Function<PaymentEvent, String> classifier) {
        if (events.isEmpty()) return;
        Map.Entry<String, Long> dominant = events.stream().collect(Collectors.groupingBy(
                        event -> normalized(classifier.apply(event)), Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
        evidence.add("%s of failures (%d/%d) have %s %s."
                .formatted(percent((double) dominant.getValue() / events.size()), dominant.getValue(),
                        events.size(), label, dominant.getKey()));
    }

    private double dominantShare(List<PaymentEvent> events, Function<PaymentEvent, String> classifier) {
        if (events.isEmpty()) return 0.0;
        long count = events.stream().collect(Collectors.groupingBy(
                        event -> normalized(classifier.apply(event)), Collectors.counting()))
                .values().stream().mapToLong(Long::longValue).max().orElse(0);
        return (double) count / events.size();
    }

    private String rateLine(String label, int count, int total) {
        double rate = total == 0 ? 0.0 : (double) count / total;
        return "%s of affected events are %s (%d/%d).".formatted(percent(rate), label, count, total);
    }
    private boolean failed(PaymentEvent event) { return "FAILED".equalsIgnoreCase(event.getStatus()); }
    private String normalized(String value) { return value == null || value.isBlank() ? "UNKNOWN" : value.toUpperCase(Locale.ROOT); }
    private String percent(double value) { return decimal(value * 100) + "%"; }
    private String decimal(double value) { return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(); }
}
