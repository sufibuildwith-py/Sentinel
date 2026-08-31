package com.sentinel.revenue.health;

import com.sentinel.revenue.detection.PaymentStatistics;
import com.sentinel.revenue.detection.StatisticsEngine;
import com.sentinel.revenue.model.PaymentDowntime;
import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.model.RecoveryJob;
import com.sentinel.revenue.repository.PaymentDowntimeRepository;
import com.sentinel.revenue.repository.PaymentEventRepository;
import com.sentinel.revenue.repository.RecoveryJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaymentHealthAnalyzer {
    private static final Map<String, Duration> CURRENT = Map.of(
            "5m", Duration.ofMinutes(5), "15m", Duration.ofMinutes(15), "60m", Duration.ofHours(1));
    private static final Map<String, Duration> BASELINE = Map.of(
            "24h", Duration.ofHours(24), "7d", Duration.ofDays(7));
    private final PaymentEventRepository payments;
    private final PaymentDowntimeRepository downtimes;
    private final RecoveryJobRepository jobs;
    private final StatisticsEngine statistics;
    private final PaymentHealthProperties properties;

    public PaymentHealthAnalyzer(PaymentEventRepository payments, PaymentDowntimeRepository downtimes,
                                 RecoveryJobRepository jobs, StatisticsEngine statistics,
                                 PaymentHealthProperties properties) {
        this.payments = payments;
        this.downtimes = downtimes;
        this.jobs = jobs;
        this.statistics = statistics;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public PaymentHealthReport analyze(String merchantId, Instant now) {
        String merchant = merchantId == null || merchantId.isBlank() ? "ALL" : merchantId;
        Instant end = now == null ? Instant.now() : now;
        List<PaymentEvent> sevenDays = payments
                .findAllByTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(
                        end.minus(Duration.ofDays(7)), end).stream()
                .filter(event -> matchesMerchant(event, merchant)).toList();
        Map<String, PaymentHealthWindow> current = windows(CURRENT, sevenDays, end);
        Map<String, PaymentHealthWindow> baseline = windows(BASELINE, sevenDays, end);
        List<PaymentHealthSignal> signals = detect(current.get("15m"), baseline.get("24h"),
                activeDowntimes(end), recentToolErrorRate(end));
        return new PaymentHealthReport(merchant, end, current, baseline, signals);
    }

    private Map<String, PaymentHealthWindow> windows(Map<String, Duration> definitions,
                                                      List<PaymentEvent> events, Instant end) {
        Map<String, PaymentHealthWindow> result = new LinkedHashMap<>();
        definitions.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(entry -> {
            List<PaymentEvent> window = events.stream()
                    .filter(event -> !event.getTimestamp().isBefore(end.minus(entry.getValue())))
                    .toList();
            result.put(entry.getKey(), window(window, entry.getValue()));
        });
        return result;
    }

    private PaymentHealthWindow window(List<PaymentEvent> events, Duration duration) {
        PaymentStatistics computed = statistics.compute(events, List.of());
        List<PaymentEvent> failures = events.stream().filter(this::failed).toList();
        return new PaymentHealthWindow(duration, events.size(), failures.size(),
                computed.amountAtRiskMinor(), computed.overallSuccessRate(),
                failures.size() / Math.max(1.0, duration.toMinutes()),
                computed.successRateByMethod(), counts(failures, PaymentEvent::getIssuerBank),
                counts(failures, PaymentEvent::getErrorCode),
                countsByHour(events), countsByDay(events));
    }

    private List<PaymentHealthSignal> detect(PaymentHealthWindow current,
                                             PaymentHealthWindow baseline,
                                             List<PaymentDowntime> activeDowntimes,
                                             double toolErrorRate) {
        List<PaymentHealthSignal> result = new ArrayList<>();
        MethodDrop methodDrop = maximumMethodDrop(current, baseline);
        result.add(signal(PaymentHealthSignalType.PAYMENT_METHOD_DEGRADATION,
                current.volume() >= properties.minimumVolume()
                        && methodDrop.drop() >= properties.successRateDrop(),
                methodDrop.drop(), 0, properties.successRateDrop(), methodDrop.method(),
                "success-rate drop=" + format(methodDrop.drop())));
        double currentFailureRate = 1 - current.successRate();
        double baselineFailureRate = 1 - baseline.successRate();
        result.add(signal(PaymentHealthSignalType.FAILURE_RATE_SPIKE,
                current.volume() >= properties.minimumVolume()
                        && currentFailureRate - baselineFailureRate >= properties.failureRateIncrease(),
                currentFailureRate, baselineFailureRate, properties.failureRateIncrease(), "merchant",
                "current failure rate=" + format(currentFailureRate)));
        double velocityThreshold = baseline.failureVelocityPerMinute()
                * properties.failureVelocityMultiplier();
        result.add(signal(PaymentHealthSignalType.FAILURE_VELOCITY_SPIKE,
                current.failures() >= properties.minimumVolume() && current.failureVelocityPerMinute() > velocityThreshold,
                current.failureVelocityPerMinute(), baseline.failureVelocityPerMinute(), velocityThreshold,
                "merchant", "failures/min=" + format(current.failureVelocityPerMinute())));
        result.add(concentration(PaymentHealthSignalType.BANK_CONCENTRATION,
                current.bankFailures(), current.failures(), "bank"));
        result.add(concentration(PaymentHealthSignalType.ERROR_REASON_CONCENTRATION,
                current.errorFailures(), current.failures(), "error"));
        result.add(signal(PaymentHealthSignalType.REVENUE_EXPOSURE_SPIKE,
                current.amountAtRiskMinor() >= properties.revenueExposureMinor(),
                current.amountAtRiskMinor(), 0, properties.revenueExposureMinor(), "merchant",
                "amount at risk minor=" + current.amountAtRiskMinor()));
        result.add(signal(PaymentHealthSignalType.PROVIDER_DOWNTIME_SIGNAL,
                !activeDowntimes.isEmpty(), activeDowntimes.size(), 0, 1, "provider",
                "active provider downtimes=" + activeDowntimes.size()));
        result.add(signal(PaymentHealthSignalType.RECOVERY_TOOL_ERROR_SPIKE,
                toolErrorRate >= properties.recoveryToolErrorRate(), toolErrorRate, 0,
                properties.recoveryToolErrorRate(), "recovery-tool",
                "recent recovery tool error rate=" + format(toolErrorRate)));
        return List.copyOf(result);
    }

    private PaymentHealthSignal concentration(PaymentHealthSignalType type, Map<String, Long> counts,
                                              int totalFailures, String dimension) {
        Map.Entry<String, Long> top = counts.entrySet().stream().max(Map.Entry.comparingByValue())
                .orElse(Map.entry("UNKNOWN", 0L));
        double share = totalFailures == 0 ? 0 : (double) top.getValue() / totalFailures;
        return signal(type, totalFailures >= properties.minimumVolume()
                        && share >= properties.concentrationThreshold(), share, 0,
                properties.concentrationThreshold(), dimension + ":" + top.getKey(),
                top.getValue() + "/" + totalFailures + " failures share=" + format(share));
    }

    private PaymentHealthSignal signal(PaymentHealthSignalType type, boolean active, double actual,
                                       double baseline, double threshold, String scope, String evidence) {
        return new PaymentHealthSignal(type, active, actual, baseline, threshold, scope,
                List.of(type + " " + (active ? "ACTIVE" : "CLEAR") + " actual=" + format(actual)
                        + " baseline=" + format(baseline) + " threshold=" + format(threshold), evidence));
    }

    private MethodDrop maximumMethodDrop(PaymentHealthWindow current, PaymentHealthWindow baseline) {
        return current.methodSuccessRates().entrySet().stream()
                .map(entry -> new MethodDrop(entry.getKey(),
                        Math.max(0, baseline.methodSuccessRates().getOrDefault(entry.getKey(), 1.0) - entry.getValue())))
                .max(Comparator.comparingDouble(MethodDrop::drop)).orElse(new MethodDrop("UNKNOWN", 0));
    }

    private List<PaymentDowntime> activeDowntimes(Instant now) {
        return downtimes.findAll().stream().filter(downtime -> downtime.getBeginAt() == null
                        || !downtime.getBeginAt().isAfter(now))
                .filter(downtime -> downtime.getEndAt() == null || downtime.getEndAt().isAfter(now))
                .filter(downtime -> downtime.getStatus() == null
                        || !"RESOLVED".equalsIgnoreCase(downtime.getStatus())).toList();
    }

    private double recentToolErrorRate(Instant now) {
        List<RecoveryJob> recent = jobs.findAll().stream()
                .filter(job -> job.getUpdatedAt() == null || !job.getUpdatedAt().isBefore(now.minus(Duration.ofMinutes(15))))
                .toList();
        if (recent.isEmpty()) return 0;
        long failures = recent.stream().filter(job -> RecoveryJob.FAILED.equals(job.getStatus())
                || RecoveryJob.EXHAUSTED.equals(job.getStatus()) || job.getErrorDetail() != null).count();
        return (double) failures / recent.size();
    }

    private boolean matchesMerchant(PaymentEvent event, String merchant) {
        return "ALL".equals(merchant) || merchant.equals(String.valueOf(event.getMetadata().get("merchantId")));
    }
    private boolean failed(PaymentEvent event) { return "FAILED".equalsIgnoreCase(event.getStatus()); }
    private Map<String, Long> counts(List<PaymentEvent> events, Function<PaymentEvent, String> key) {
        return events.stream().collect(Collectors.groupingBy(event -> normalized(key.apply(event)),
                TreeMap::new, Collectors.counting()));
    }
    private Map<Integer, Long> countsByHour(List<PaymentEvent> events) {
        return events.stream().collect(Collectors.groupingBy(event -> event.getTimestamp().atZone(ZoneOffset.UTC).getHour(),
                TreeMap::new, Collectors.counting()));
    }
    private Map<String, Long> countsByDay(List<PaymentEvent> events) {
        return events.stream().collect(Collectors.groupingBy(event -> event.getTimestamp().atZone(ZoneOffset.UTC)
                .getDayOfWeek().name(), TreeMap::new, Collectors.counting()));
    }
    private String normalized(String value) { return value == null || value.isBlank() ? "UNKNOWN" : value.toUpperCase(Locale.ROOT); }
    private String format(double value) { return String.format(Locale.ROOT, "%.4f", value); }
    private record MethodDrop(String method, double drop) { }
}
