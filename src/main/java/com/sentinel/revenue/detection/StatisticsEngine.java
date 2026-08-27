package com.sentinel.revenue.detection;

import com.sentinel.revenue.model.PaymentEvent;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StatisticsEngine {

    private static final String UNKNOWN = "UNKNOWN";

    private final DetectionProperties properties;
    private final Set<String> successStatuses;

    public StatisticsEngine(DetectionProperties properties) {
        this.properties = properties;
        this.successStatuses = properties.successStatuses().stream()
                .map(status -> status.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public PaymentStatistics compute(List<PaymentEvent> windowEvents,
                                     List<PaymentEvent> baselineEvents) {
        List<PaymentEvent> window = List.copyOf(windowEvents);
        List<PaymentEvent> baseline = List.copyOf(baselineEvents);

        int eventCount = window.size();
        int failedEventCount = (int) window.stream().filter(this::isFailed).count();
        long totalValueMinor = sumAmount(window);
        long amountAtRiskMinor = sumAmount(window.stream().filter(this::isFailed).toList());
        double successRate = rate(window, this::isSuccessful);
        BaselineStatistics baselineStatistics = baselineStatistics(baseline);

        double standardDeviation = Math.max(
                baselineStatistics.standardDeviation(),
                properties.minimumBaselineStandardDeviation());
        double deviation = Math.max(0.0,
                (baselineStatistics.mean() - successRate) / standardDeviation);

        return new PaymentStatistics(
                eventCount,
                failedEventCount,
                totalValueMinor,
                amountAtRiskMinor,
                successRate,
                groupedSuccessRates(window, PaymentEvent::getMethod),
                groupedSuccessRates(window, PaymentEvent::getIssuerBank),
                failureDistribution(window),
                rate(window, this::isRetry),
                valueConcentration(window, totalValueMinor),
                rate(window, this::isAbandoned),
                baselineStatistics.mean(),
                standardDeviation,
                deviation,
                baselineStatistics.bucketCount());
    }

    private Map<String, Double> groupedSuccessRates(
            List<PaymentEvent> events,
            Function<PaymentEvent, String> classifier) {
        Map<String, List<PaymentEvent>> grouped = events.stream()
                .collect(Collectors.groupingBy(
                        event -> normalized(classifier.apply(event)),
                        TreeMap::new,
                        Collectors.toList()));
        Map<String, Double> rates = new LinkedHashMap<>();
        grouped.forEach((key, values) -> rates.put(key, rate(values, this::isSuccessful)));
        return rates;
    }

    private Map<String, Long> failureDistribution(List<PaymentEvent> events) {
        return events.stream()
                .filter(this::isFailed)
                .collect(Collectors.groupingBy(
                        event -> normalized(event.getErrorCode()),
                        TreeMap::new,
                        Collectors.counting()));
    }

    private BaselineStatistics baselineStatistics(List<PaymentEvent> baselineEvents) {
        DescriptiveStatistics statistics = new DescriptiveStatistics();
        if (!baselineEvents.isEmpty()) {
            long bucketMillis = properties.baselineBucket().toMillis();
            baselineEvents.stream()
                    .collect(Collectors.groupingBy(
                            event -> event.getTimestamp().toEpochMilli() / bucketMillis,
                            TreeMap::new,
                            Collectors.toList()))
                    .values()
                    .stream()
                    .mapToDouble(events -> rate(events, this::isSuccessful))
                    .forEach(statistics::addValue);
        }

        if (statistics.getN() == 0) {
            statistics.addValue(properties.defaultBaselineSuccessRate());
        }
        double standardDeviation = statistics.getN() < 2
                ? 0.0 : statistics.getStandardDeviation();
        if (Double.isNaN(standardDeviation)) {
            standardDeviation = 0.0;
        }
        return new BaselineStatistics(
                statistics.getMean(), standardDeviation, (int) statistics.getN());
    }

    private double valueConcentration(List<PaymentEvent> events, long totalValueMinor) {
        if (events.isEmpty() || totalValueMinor == 0) {
            return 0.0;
        }
        int topCount = Math.max(1, (int) Math.ceil(events.size() * 0.10));
        List<Long> amounts = new ArrayList<>(events.stream()
                .map(PaymentEvent::getAmountMinor)
                .toList());
        amounts.sort(Comparator.reverseOrder());
        long topValue = 0;
        for (int index = 0; index < topCount; index++) {
            topValue = Math.addExact(topValue, amounts.get(index));
        }
        return (double) topValue / totalValueMinor;
    }

    private long sumAmount(List<PaymentEvent> events) {
        long total = 0;
        for (PaymentEvent event : events) {
            total = Math.addExact(total, event.getAmountMinor());
        }
        return total;
    }

    private double rate(List<PaymentEvent> events,
                        java.util.function.Predicate<PaymentEvent> predicate) {
        if (events.isEmpty()) {
            return 0.0;
        }
        return (double) events.stream().filter(predicate).count() / events.size();
    }

    private boolean isSuccessful(PaymentEvent event) {
        return successStatuses.contains(normalized(event.getStatus()));
    }

    private boolean isFailed(PaymentEvent event) {
        return "FAILED".equals(normalized(event.getStatus()));
    }

    private boolean isAbandoned(PaymentEvent event) {
        return "ABANDONED".equals(normalized(event.getStatus()));
    }

    private boolean isRetry(PaymentEvent event) {
        return event.getAttemptNumber() > 1 || event.getPreviousFailureCount() > 0;
    }

    private String normalized(String value) {
        return value == null || value.isBlank()
                ? UNKNOWN : value.trim().toUpperCase(Locale.ROOT);
    }

    private record BaselineStatistics(
            double mean,
            double standardDeviation,
            int bucketCount) {
    }
}
