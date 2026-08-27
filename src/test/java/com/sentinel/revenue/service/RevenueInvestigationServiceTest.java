package com.sentinel.revenue.service;

import com.sentinel.core.llm.EmbeddingClient;
import com.sentinel.core.llm.LlmClient;
import com.sentinel.revenue.detection.DetectionProperties;
import com.sentinel.revenue.detection.StatisticsEngine;
import com.sentinel.revenue.investigation.*;
import com.sentinel.revenue.model.FindingSource;
import com.sentinel.revenue.model.IncidentFinding;
import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.repository.HistoricalIncidentRepository;
import com.sentinel.revenue.repository.IncidentFindingRepository;
import com.sentinel.revenue.repository.PaymentEventRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RevenueInvestigationServiceTest {
    @Test
    void labelledAnomaliesReachDiagnosedWithAccurateFallbackWhenLlmIsDown() {
        assertDiagnosis("UPI_DEGRADATION", "UPI", "HDFC", "UPI_ISSUER_UNAVAILABLE",
                "UPI issuer degradation");
        assertDiagnosis("PROVIDER_OUTAGE", "CARD", "MULTIPLE", "PROVIDER_UNAVAILABLE",
                "Payment provider outage");
    }

    private void assertDiagnosis(String type, String method, String issuer, String error,
                                 String expectedRootCause) {
        RevenueIncidentRepository incidentRepository = mock(RevenueIncidentRepository.class);
        IncidentFindingRepository findingRepository = mock(IncidentFindingRepository.class);
        PaymentEventRepository paymentRepository = mock(PaymentEventRepository.class);
        HistoricalIncidentRepository historyRepository = mock(HistoricalIncidentRepository.class);
        LlmClient downLlm = mock(LlmClient.class);
        EmbeddingClient embeddings = mock(EmbeddingClient.class);
        when(downLlm.generateStructured(any(), eq(RootCauseResult.class)))
                .thenThrow(new RuntimeException("Gemini unavailable"));
        when(historyRepository.findAll()).thenReturn(List.of());

        UUID id = UUID.randomUUID();
        List<String> paymentIds = new ArrayList<>();
        List<PaymentEvent> events = new ArrayList<>();
        Instant detected = Instant.parse("2026-08-27T10:00:00Z");
        for (int index = 0; index < 12; index++) {
            String paymentId = "fixture-" + type + "-" + index;
            paymentIds.add(paymentId);
            events.add(new PaymentEvent(paymentId, "order-" + index, "customer-" + index,
                    20000, "INR", method, issuer, "FAILED", error, "fixture failure",
                    detected.minusSeconds(index), 1, "CARD", 0, null, Map.of("merchantId", "demo")));
        }
        RevenueIncident incident = new RevenueIncident(type, RevenueIncidentStatus.DETECTED,
                "HIGH", 240000, detected, paymentIds,
                events.stream().map(PaymentEvent::getCustomerId).toList(),
                List.of("MIN_VOLUME PASS actual=12 threshold=10", "AMOUNT_AT_RISK PASS actual=240000"),
                null, null);
        ReflectionTestUtils.setField(incident, "incidentId", id);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(paymentRepository.findAllByPaymentIdIn(any())).thenReturn(events);
        when(paymentRepository.findAllByTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(any(), any()))
                .thenReturn(List.of());
        List<IncidentFinding> persisted = new ArrayList<>();
        when(findingRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            IncidentFinding finding = invocation.getArgument(0);
            persisted.add(finding);
            return finding;
        });

        DetectionProperties detection = new DetectionProperties(Duration.ofHours(1), Duration.ofHours(1),
                Duration.ofHours(24), Duration.ofMinutes(15), 10, 0.2, 2, 100000,
                0.95, 0.05, Set.of("CAPTURED", "AUTHORIZED"), "merchantId");
        PatternAnalyzer pattern = new PatternAnalyzer(paymentRepository,
                new StatisticsEngine(detection), detection);
        CustomerContextTool customer = new CustomerContextTool(paymentRepository, historyRepository);
        PaymentAnalystAgent analyst = new PaymentAnalystAgent(pattern, customer);
        InvestigationProperties investigationProperties = new InvestigationProperties(
                Duration.ofSeconds(1), 5, 0.0, 50, 2, 4, Duration.ofSeconds(30));
        StructuredLlmGateway gateway = new StructuredLlmGateway(downLlm, investigationProperties);
        try {
            RootCauseAgent root = new RootCauseAgent(new PromptContextBuilder(), gateway,
                    Validation.buildDefaultValidatorFactory().getValidator());
            HistoricalMemoryService memory = new HistoricalMemoryService(historyRepository, embeddings,
                    investigationProperties);
            RevenueInvestigationService service = new RevenueInvestigationService(incidentRepository,
                    findingRepository, new TriageAgent(), analyst, memory, root);

            InvestigationReport report = service.investigate(id);

            assertThat(report.status()).isEqualTo(RevenueIncidentStatus.DIAGNOSED);
            assertThat(report.diagnosis().rootCause()).isEqualTo(expectedRootCause);
            assertThat(report.diagnosis().llmUnavailable()).isTrue();
            assertThat(incident.getStatus()).isEqualTo(RevenueIncidentStatus.DIAGNOSED);
            assertThat(persisted).extracting(IncidentFinding::getSource)
                    .containsExactly(FindingSource.PAYMENT_ANALYST, FindingSource.ROOT_CAUSE_AGENT);
            verify(downLlm, times(2)).generateStructured(any(), eq(RootCauseResult.class));
        } finally {
            gateway.close();
        }
    }
}
