package com.sentinel.revenue.service;

import com.sentinel.revenue.detection.DetectionDecision;
import com.sentinel.revenue.detection.DetectionProperties;
import com.sentinel.revenue.detection.DetectionRuleEngine;
import com.sentinel.revenue.detection.FailureCluster;
import com.sentinel.revenue.detection.FailureClusterKey;
import com.sentinel.revenue.detection.PaymentStatistics;
import com.sentinel.revenue.detection.StatisticsEngine;
import com.sentinel.revenue.audit.AuditLogService;
import com.sentinel.revenue.model.FindingSource;
import com.sentinel.revenue.model.IncidentFinding;
import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.repository.IncidentFindingRepository;
import com.sentinel.revenue.repository.PaymentEventRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FailureClusteringService implements PaymentEventBatchListener {

    private static final String UNKNOWN = "UNKNOWN";
    private static final String FINGERPRINT_PREFIX = "Cluster fingerprint: ";

    private final PaymentEventRepository paymentEvents;
    private final RevenueIncidentRepository incidents;
    private final IncidentFindingRepository findings;
    private final StatisticsEngine statisticsEngine;
    private final DetectionRuleEngine ruleEngine;
    private final DetectionProperties properties;
    private final AuditLogService audit;

    @Autowired
    public FailureClusteringService(PaymentEventRepository paymentEvents,
                                    RevenueIncidentRepository incidents,
                                    IncidentFindingRepository findings,
                                    StatisticsEngine statisticsEngine,
                                    DetectionRuleEngine ruleEngine,
                                    DetectionProperties properties,
                                    AuditLogService audit) {
        this.paymentEvents = paymentEvents;
        this.incidents = incidents;
        this.findings = findings;
        this.statisticsEngine = statisticsEngine;
        this.ruleEngine = ruleEngine;
        this.properties = properties;
        this.audit = audit;
    }

    public FailureClusteringService(PaymentEventRepository paymentEvents,
                                    RevenueIncidentRepository incidents,
                                    IncidentFindingRepository findings,
                                    StatisticsEngine statisticsEngine,
                                    DetectionRuleEngine ruleEngine,
                                    DetectionProperties properties) {
        this(paymentEvents, incidents, findings, statisticsEngine, ruleEngine, properties, null);
    }

    @Override
    @Transactional
    public void onEventsPersisted(List<PaymentEvent> events) {
        detectNewEvents(events);
    }

    @Transactional
    public List<RevenueIncident> detectNewEvents(List<PaymentEvent> newlyPersisted) {
        if (newlyPersisted.isEmpty()) {
            return List.of();
        }

        Set<UUID> newIds = newlyPersisted.stream()
                .map(PaymentEvent::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Instant first = newlyPersisted.stream().map(PaymentEvent::getTimestamp)
                .min(Comparator.naturalOrder()).orElseThrow();
        Instant last = newlyPersisted.stream().map(PaymentEvent::getTimestamp)
                .max(Comparator.naturalOrder()).orElseThrow();
        List<PaymentEvent> candidates = paymentEvents
                .findAllByTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(
                        first.minus(properties.clusterWindow()), last.plusMillis(1));

        Set<String> existingFingerprints = incidents.findAll().stream()
                .flatMap(incident -> incident.getEvidence().stream())
                .filter(evidence -> evidence.startsWith(FINGERPRINT_PREFIX))
                .map(evidence -> evidence.substring(FINGERPRINT_PREFIX.length()))
                .collect(Collectors.toCollection(HashSet::new));

        List<RevenueIncident> created = new ArrayList<>();
        for (FailureCluster cluster : clusterFailures(candidates)) {
            boolean containsNewEvent = cluster.contributingEvents().stream()
                    .map(PaymentEvent::getId)
                    .anyMatch(newIds::contains);
            if (!containsNewEvent || existingFingerprints.contains(cluster.fingerprint())) {
                continue;
            }

            PaymentStatistics statistics = statisticsFor(cluster);
            DetectionDecision decision = ruleEngine.evaluate(statistics);
            if (!decision.incidentRequired()) {
                continue;
            }

            RevenueIncident incident = persistIncident(cluster, statistics, decision);
            created.add(incident);
            existingFingerprints.add(cluster.fingerprint());
        }
        return List.copyOf(created);
    }

    public List<FailureCluster> clusterFailures(List<PaymentEvent> events) {
        Map<FailureClusterKey, List<PaymentEvent>> grouped = events.stream()
                .filter(this::isFailed)
                .sorted(Comparator.comparing(PaymentEvent::getTimestamp))
                .collect(Collectors.groupingBy(
                        this::clusterKey,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<FailureCluster> clusters = new ArrayList<>();
        grouped.forEach((key, failures) -> {
            List<PaymentEvent> current = new ArrayList<>();
            Instant windowStart = null;
            for (PaymentEvent failure : failures) {
                if (windowStart == null || outsideWindow(windowStart, failure.getTimestamp())) {
                    addCluster(clusters, key, current);
                    current = new ArrayList<>();
                    windowStart = failure.getTimestamp();
                }
                current.add(failure);
            }
            addCluster(clusters, key, current);
        });
        clusters.sort(Comparator.comparing(FailureCluster::windowStart));
        return List.copyOf(clusters);
    }

    private PaymentStatistics statisticsFor(FailureCluster cluster) {
        Instant endExclusive = cluster.windowStart()
                .plus(properties.evaluationWindow())
                .plusNanos(1);
        List<PaymentEvent> cohort = paymentEvents
                .findAllByTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(
                        cluster.windowStart(), endExclusive)
                .stream()
                .filter(event -> sameContext(event, cluster.key()))
                .filter(event -> !isFailed(event)
                        || normalized(event.getErrorCode()).equals(cluster.key().errorCode()))
                .toList();

        Instant baselineEnd = cluster.windowStart();
        List<PaymentEvent> baseline = paymentEvents
                .findAllByTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(
                        baselineEnd.minus(properties.baselineWindow()), baselineEnd)
                .stream()
                .filter(event -> sameContext(event, cluster.key()))
                .toList();
        return statisticsEngine.compute(cohort, baseline);
    }

    private RevenueIncident persistIncident(FailureCluster cluster,
                                            PaymentStatistics statistics,
                                            DetectionDecision decision) {
        List<String> contributingPaymentIds = cluster.contributingEvents().stream()
                .map(PaymentEvent::getPaymentId)
                .distinct()
                .sorted()
                .toList();
        List<String> affectedCustomers = cluster.contributingEvents().stream()
                .map(PaymentEvent::getCustomerId)
                .distinct()
                .sorted()
                .toList();
        List<String> contributingEventIds = cluster.contributingEvents().stream()
                .map(PaymentEvent::getId)
                .map(UUID::toString)
                .sorted()
                .toList();

        List<String> evidence = new ArrayList<>();
        evidence.add(FINGERPRINT_PREFIX + cluster.fingerprint());
        decision.rules().forEach(result -> evidence.add(result.evidenceLine()));
        evidence.add("Contributing event IDs (%d): %s".formatted(
                contributingEventIds.size(), String.join(", ", contributingEventIds)));
        evidence.add("Observed success rate=%s; baseline=%s; retry frequency=%s; "
                        .formatted(decimal(statistics.overallSuccessRate()),
                                decimal(statistics.baselineSuccessRate()),
                                decimal(statistics.retryFrequency()))
                + "abandonment rate=" + decimal(statistics.abandonmentRate())
                + "; value concentration=" + decimal(statistics.valueConcentration()));

        String type = incidentType(cluster.key());
        String severity = statistics.amountAtRiskMinor()
                / 5 >= properties.minimumAmountAtRiskMinor() ? "HIGH" : "MEDIUM";
        RevenueIncident incident = incidents.saveAndFlush(new RevenueIncident(
                type,
                RevenueIncidentStatus.DETECTED,
                severity,
                statistics.amountAtRiskMinor(),
                cluster.windowEnd(),
                contributingPaymentIds,
                affectedCustomers,
                evidence,
                null,
                null));

        String summary = "%s detected for method=%s, issuer=%s, error=%s, merchant=%s: "
                .formatted(type, cluster.key().method(), cluster.key().issuer(),
                        cluster.key().errorCode(), cluster.key().merchantId())
                + "%d contributing failed events, %d affected customers, %d minor units at risk; "
                .formatted(contributingEventIds.size(), affectedCustomers.size(),
                        statistics.amountAtRiskMinor())
                + "all %d detection rules passed.".formatted(decision.rules().size());
        findings.saveAndFlush(new IncidentFinding(
                incident, FindingSource.DETECTOR, summary, null, evidence, cluster.windowEnd()));
        if (audit != null) {
            audit.append(incident, "DETECTOR", null, "INCIDENT_DETECTED", evidence, null,
                    summary, decision.rules().stream().map(result -> result.evidenceLine()).toList(),
                    "DETECTED", null, RevenueIncidentStatus.DETECTED,
                    statistics.amountAtRiskMinor() + " minor units at risk");
        }
        return incident;
    }

    private void addCluster(List<FailureCluster> clusters, FailureClusterKey key,
                            List<PaymentEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        clusters.add(new FailureCluster(
                key,
                events.get(0).getTimestamp(),
                events.get(events.size() - 1).getTimestamp(),
                events));
    }

    private boolean outsideWindow(Instant windowStart, Instant eventTime) {
        return Duration.between(windowStart, eventTime)
                .compareTo(properties.clusterWindow()) >= 0;
    }

    private FailureClusterKey clusterKey(PaymentEvent event) {
        return new FailureClusterKey(
                normalized(event.getMethod()),
                normalized(event.getIssuerBank()),
                normalized(event.getErrorCode()),
                merchantId(event));
    }

    private boolean sameContext(PaymentEvent event, FailureClusterKey key) {
        return normalized(event.getMethod()).equals(key.method())
                && normalized(event.getIssuerBank()).equals(key.issuer())
                && merchantId(event).equals(key.merchantId());
    }

    private String merchantId(PaymentEvent event) {
        Object merchant = event.getMetadata().get(properties.merchantMetadataKey());
        return normalized(merchant == null ? null : merchant.toString());
    }

    private String incidentType(FailureClusterKey key) {
        return switch (key.errorCode()) {
            case "UPI_ISSUER_UNAVAILABLE" -> "UPI_DEGRADATION";
            case "PROVIDER_UNAVAILABLE" -> "PROVIDER_OUTAGE";
            default -> "PAYMENT_FAILURE_CLUSTER";
        };
    }

    private boolean isFailed(PaymentEvent event) {
        return "FAILED".equals(normalized(event.getStatus()));
    }

    private String normalized(String value) {
        return value == null || value.isBlank()
                ? UNKNOWN : value.trim().toUpperCase(Locale.ROOT);
    }

    private String decimal(double value) {
        return java.math.BigDecimal.valueOf(value)
                .setScale(4, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
