package com.sentinel.revenue.metrics;

import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FinancialAttributionServiceTest {
    @Test
    void providerEventsCannotDoubleCountTheSamePayment() {
        RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        PaymentEventRepository payments = mock(PaymentEventRepository.class);
        IncidentFindingRepository findings = mock(IncidentFindingRepository.class);
        RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
        RecoveryOutcomeRepository outcomes = mock(RecoveryOutcomeRepository.class);
        RecoveryCostEntryRepository costs = mock(RecoveryCostEntryRepository.class);
        RecoveryAction first = action("payment-1");
        RecoveryAction second = action("payment-1");
        RecoveryOutcome captured = outcome(first.getId(), "evt-payment-captured", 10_000);
        RecoveryOutcome orderPaid = outcome(second.getId(), "evt-order-paid", 10_000);
        when(incidents.findAll()).thenReturn(List.of());
        when(actions.findAll()).thenReturn(List.of(first, second));
        when(outcomes.findAll()).thenReturn(List.of(captured, orderPaid));
        when(outcomes.findByRecoveryActionId(any())).thenReturn(Optional.empty());
        when(costs.findAll()).thenReturn(List.of());

        FinancialAttribution result = new FinancialAttributionService(incidents, payments,
                findings, actions, outcomes, costs).attribution();

        assertThat(result.executedValueMinor()).isEqualTo(20_000);
        assertThat(result.providerConfirmedRecoveryMinor()).isEqualTo(10_000);
        assertThat(result.attributedIncrementalRecoveryMinor()).isEqualTo(10_000);
        assertThat(result.naturalRecoveryEstimationStatus()).isEqualTo("NOT_ESTIMATED_NO_CAUSAL_BASELINE");
    }

    private RecoveryAction action(String paymentId) {
        RecoveryAction action = mock(RecoveryAction.class);
        when(action.getId()).thenReturn(UUID.randomUUID());
        when(action.getIncidentId()).thenReturn(UUID.randomUUID());
        when(action.getTargetPaymentId()).thenReturn(paymentId);
        when(action.getExternalResourceId()).thenReturn("plink-" + UUID.randomUUID());
        when(action.getAmountMinor()).thenReturn(10_000L);
        when(action.getStatus()).thenReturn(RecoveryActionStatus.EXECUTED);
        return action;
    }

    private RecoveryOutcome outcome(UUID actionId, String sourceEvent, long amount) {
        RecoveryOutcome outcome = mock(RecoveryOutcome.class);
        when(outcome.isProviderConfirmed()).thenReturn(true);
        when(outcome.getRecoveryActionId()).thenReturn(actionId);
        when(outcome.getSourceEventId()).thenReturn(sourceEvent);
        when(outcome.getRecoveredAmountMinor()).thenReturn(amount);
        return outcome;
    }
}
