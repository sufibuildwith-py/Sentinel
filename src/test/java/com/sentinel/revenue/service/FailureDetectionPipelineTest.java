package com.sentinel.revenue.service;

import com.sentinel.revenue.api.PaymentEventRequest;
import com.sentinel.revenue.dataset.SyntheticPaymentDatasetGenerator;
import com.sentinel.revenue.detection.DetectionProperties;
import com.sentinel.revenue.detection.DetectionRuleEngine;
import com.sentinel.revenue.detection.StatisticsEngine;
import com.sentinel.revenue.model.FindingSource;
import com.sentinel.revenue.model.IncidentFinding;
import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.repository.IncidentFindingRepository;
import com.sentinel.revenue.repository.PaymentEventRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FailureDetectionPipelineTest {

    private final PaymentEventRepository paymentEvents = mock(PaymentEventRepository.class);
    private final RevenueIncidentRepository incidentRepository = mock(RevenueIncidentRepository.class);
    private final IncidentFindingRepository findingRepository = mock(IncidentFindingRepository.class);
    private final List<RevenueIncident> incidents = new ArrayList<>();
    private final List<IncidentFinding> findings = new ArrayList<>();
    private List<PaymentEvent> dataset;
    private FailureClusteringService service;

    @BeforeEach
    void setUp() {
        DetectionProperties properties = new DetectionProperties(
                Duration.ofHours(1), Duration.ofHours(1), Duration.ofHours(24),
                Duration.ofMinutes(15), 10, 0.20, 2.0, 100_000,
                0.95, 0.05, Set.of("CAPTURED", "AUTHORIZED"), "merchantId");
        dataset = uniqueDataset();

        when(paymentEvents
                .findAllByTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(
                        any(Instant.class), any(Instant.class)))
                .thenAnswer(invocation -> eventsBetween(
                        invocation.getArgument(0), invocation.getArgument(1)));
        when(incidentRepository.findAll()).thenAnswer(invocation -> List.copyOf(incidents));
        when(incidentRepository.saveAndFlush(any(RevenueIncident.class)))
                .thenAnswer(invocation -> {
                    RevenueIncident incident = invocation.getArgument(0);
                    ReflectionTestUtils.setField(incident, "incidentId", UUID.randomUUID());
                    incidents.add(incident);
                    return incident;
                });
        when(findingRepository.saveAndFlush(any(IncidentFinding.class)))
                .thenAnswer(invocation -> {
                    IncidentFinding finding = invocation.getArgument(0);
                    findings.add(finding);
                    return finding;
                });

        service = new FailureClusteringService(
                paymentEvents, incidentRepository, findingRepository,
                new StatisticsEngine(properties), new DetectionRuleEngine(properties),
                properties);
    }

    @Test
    void fullLabelledDatasetCreatesOnlyExplainableUpiAndProviderIncidents() {
        List<RevenueIncident> created = service.detectNewEvents(dataset);

        assertThat(created)
                .extracting(RevenueIncident::getType)
                .containsExactlyInAnyOrder("UPI_DEGRADATION", "PROVIDER_OUTAGE");
        assertThat(created).noneMatch(incident ->
                incident.getType().equals("NORMAL_FAILURE_MIX"));
        assertThat(findings).hasSize(2).allSatisfy(finding -> {
            assertThat(finding.getSource()).isEqualTo(FindingSource.DETECTOR);
            assertThat(finding.getSummary())
                    .contains("contributing failed events")
                    .contains("minor units at risk")
                    .contains("all 4 detection rules passed");
            assertThat(finding.getEvidence())
                    .filteredOn(line -> line.startsWith("PASS "))
                    .hasSize(4);
            assertThat(finding.getEvidence())
                    .anyMatch(line -> line.startsWith("Contributing event IDs ("));
        });
    }

    private List<PaymentEvent> uniqueDataset() {
        Map<String, PaymentEvent> unique = new LinkedHashMap<>();
        for (PaymentEventRequest request : new SyntheticPaymentDatasetGenerator()
                .generate().events()) {
            String key = request.paymentId() + ":" + request.attemptNumber();
            unique.computeIfAbsent(key, ignored -> entity(request));
        }
        return unique.values().stream()
                .sorted(Comparator.comparing(PaymentEvent::getTimestamp))
                .toList();
    }

    private PaymentEvent entity(PaymentEventRequest request) {
        PaymentEvent event = new PaymentEvent(
                request.paymentId(), request.orderId(), request.customerId(),
                request.amountMinor(), request.currency(), request.method(),
                request.issuerBank(), request.status(), request.errorCode(),
                request.errorDescription(), request.timestamp(), request.attemptNumber(),
                request.previousSuccessfulMethod(), request.previousFailureCount(),
                request.subscriptionId(), request.metadata());
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        return event;
    }

    private List<PaymentEvent> eventsBetween(Instant startInclusive, Instant endExclusive) {
        return dataset.stream()
                .filter(event -> !event.getTimestamp().isBefore(startInclusive))
                .filter(event -> event.getTimestamp().isBefore(endExclusive))
                .toList();
    }
}
