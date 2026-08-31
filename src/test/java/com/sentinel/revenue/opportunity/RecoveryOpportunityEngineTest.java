package com.sentinel.revenue.opportunity;

import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecoveryOpportunityEngineTest {
    @Test
    void m0IsShadowOnlyAndDoesNotManufactureCausalEstimatesOrCapabilities() {
        HistoricalIncidentRepository history = mock(HistoricalIncidentRepository.class);
        RecoveryOpportunityLogRepository logs = mock(RecoveryOpportunityLogRepository.class);
        when(history.findAll()).thenReturn(List.of());
        when(logs.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        RevenueIncident incident = new RevenueIncident("UPI_DEGRADATION", RevenueIncidentStatus.DIAGNOSED,
                "HIGH", 250_000, Instant.now(), List.of("payment-1"), List.of("customer-1"),
                List.of(), "UPI outage", null);
        ReflectionTestUtils.setField(incident, "incidentId", UUID.randomUUID());
        RecoveryOpportunityEngine engine = new RecoveryOpportunityEngine(new ProviderCapabilityRegistry(),
                history, logs, new OpportunityProperties(CausalMaturity.M0, "SHADOW_ONLY"));

        RecoveryOpportunityDecision decision = engine.evaluate(incident, RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK);

        assertThat(decision.mode()).isEqualTo("SHADOW_ONLY");
        assertThat(decision.maturity()).isEqualTo(CausalMaturity.M0);
        assertThat(decision.candidates()).extracting(ActionOpportunity::action)
                .contains(OpportunityAction.NO_ACTION, OpportunityAction.CREATE_PAYMENT_LINK)
                .doesNotContainNull();
        assertThat(decision.candidates()).allSatisfy(candidate -> {
            assertThat(candidate.naturalRecoveryProbability()).isNull();
            assertThat(candidate.incrementalUplift()).isNull();
            assertThat(candidate.netIncrementalValueMinor()).isNull();
            assertThat(candidate.estimateKind()).isEqualTo("UNAVAILABLE_M0_NO_CAUSAL_MODEL");
        });
        assertThat(decision.candidates()).extracting(candidate -> candidate.action().name())
                .doesNotContain("RETRY_FAILED_PAYMENT");
    }
}
