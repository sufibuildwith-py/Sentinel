package com.sentinel.revenue.investigation;

import com.sentinel.core.llm.EmbeddingClient;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.HistoricalIncidentRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HistoricalMemoryServiceTest {
    private final InvestigationProperties properties = new InvestigationProperties(
            Duration.ofSeconds(1), 5, 0.0, 50, 2, 4, Duration.ofSeconds(30));

    @Test
    void emptyMemoryIsNotAnErrorAndDoesNotCallEmbeddingProvider() {
        HistoricalIncidentRepository repository = mock(HistoricalIncidentRepository.class);
        EmbeddingClient embeddings = mock(EmbeddingClient.class);
        when(repository.findAll()).thenReturn(List.of());

        List<SimilarHistoricalIncident> matches = new HistoricalMemoryService(
                repository, embeddings, properties).findSimilar(incident());

        assertThat(matches).isEmpty();
        verifyNoInteractions(embeddings);
    }

    @Test
    void computesRecoveryRateFromPersistedHistoricalAmounts() {
        HistoricalIncidentRepository repository = mock(HistoricalIncidentRepository.class);
        EmbeddingClient embeddings = text -> new float[]{1, 0};
        HistoricalIncident historical = new HistoricalIncident(null, "UPI issuer timeout",
                Map.of("amountAtRiskMinor", 100000L), RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK,
                RecoveryOutcomeStatus.RECOVERED, 72300, Instant.now());
        when(repository.findAll()).thenReturn(List.of(historical));

        List<SimilarHistoricalIncident> matches = new HistoricalMemoryService(
                repository, embeddings, properties).findSimilar(incident());

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).recoveryRate()).isEqualTo(0.723);
        assertThat(matches.get(0).recoveredAmountMinor()).isEqualTo(72300);
    }

    private RevenueIncident incident() {
        return new RevenueIncident("UPI_DEGRADATION", RevenueIncidentStatus.DETECTED, "HIGH",
                100000, Instant.now(), List.of("p1"), List.of("c1"), List.of("issuer failures"), null, null);
    }
}
