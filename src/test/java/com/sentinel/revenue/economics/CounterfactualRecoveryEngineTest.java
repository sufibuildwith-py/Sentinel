package com.sentinel.revenue.economics;

import com.sentinel.revenue.model.HistoricalIncident;
import com.sentinel.revenue.model.RecoveryOutcomeStatus;
import com.sentinel.revenue.model.RecoveryStrategy;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.opportunity.CausalMaturity;
import com.sentinel.revenue.opportunity.OpportunityAction;
import com.sentinel.revenue.repository.HistoricalIncidentRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CounterfactualRecoveryEngineTest {
    @Test
    void m0ReturnsUnknownInsteadOfManufacturingEconomics() {
        HistoricalIncidentRepository history = mock(HistoricalIncidentRepository.class);
        CounterfactualEstimate estimate = new CounterfactualRecoveryEngine(history)
                .estimate(mock(RevenueIncident.class), OpportunityAction.NO_ACTION, CausalMaturity.M0);

        assertThat(estimate.naturalRecoveryProbability()).isNull();
        assertThat(estimate.estimatedIncrementalRecoveryMinor()).isNull();
        assertThat(estimate.evidenceQuality()).isEqualTo(EconomicEvidenceQuality.NOT_ESTIMATED);
        verifyNoInteractions(history);
    }

    @Test
    void m1ReportsObservationalFrequenciesWithoutCausalUplift() {
        HistoricalIncidentRepository history = mock(HistoricalIncidentRepository.class);
        HistoricalIncident noActionRecovered = historical(RecoveryStrategy.NO_ACTION, RecoveryOutcomeStatus.RECOVERED);
        HistoricalIncident noActionFailed = historical(RecoveryStrategy.NO_ACTION, RecoveryOutcomeStatus.FAILED);
        HistoricalIncident linkRecovered = historical(RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK, RecoveryOutcomeStatus.RECOVERED);
        when(history.findAll()).thenReturn(List.of(noActionRecovered, noActionFailed, linkRecovered));

        CounterfactualEstimate estimate = new CounterfactualRecoveryEngine(history)
                .estimate(mock(RevenueIncident.class), OpportunityAction.CREATE_PAYMENT_LINK, CausalMaturity.M1);

        assertThat(estimate.naturalRecoveryProbability()).isEqualByComparingTo("0.5000");
        assertThat(estimate.actionRecoveryProbability()).isEqualByComparingTo("1.0000");
        assertThat(estimate.estimatedIncrementalRecoveryMinor()).isNull();
        assertThat(estimate.estimatedNetIncrementalValueMinor()).isNull();
        assertThat(estimate.method()).contains("NOT_CAUSAL");
        assertThat(estimate.naturalRecoveryInterval().method()).isEqualTo("WILSON_95_PERCENT");
    }

    private HistoricalIncident historical(RecoveryStrategy strategy, RecoveryOutcomeStatus outcome) {
        HistoricalIncident item = mock(HistoricalIncident.class);
        when(item.getRecoveryStrategy()).thenReturn(strategy);
        when(item.getOutcome()).thenReturn(outcome);
        when(item.getCreatedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        return item;
    }
}
