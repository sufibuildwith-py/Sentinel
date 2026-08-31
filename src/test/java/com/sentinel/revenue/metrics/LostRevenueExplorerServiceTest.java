package com.sentinel.revenue.metrics;

import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RecoveryActionStatus;
import com.sentinel.revenue.model.RecoveryOutcome;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.repository.RecoveryActionRepository;
import com.sentinel.revenue.repository.RecoveryOutcomeRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LostRevenueExplorerServiceTest {
    @Test
    void categorizesEachOpenIncidentOnceAndCreditsOnlyProviderConfirmedTruth() {
        RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
        RecoveryOutcomeRepository outcomes = mock(RecoveryOutcomeRepository.class);
        UUID blockedId = UUID.randomUUID();
        UUID awaitingId = UUID.randomUUID();
        UUID actionBlockedId = UUID.randomUUID();
        UUID actionAwaitingId = UUID.randomUUID();
        RevenueIncident blocked = incident(blockedId, 10_000L);
        RevenueIncident awaiting = incident(awaitingId, 20_000L);
        RecoveryAction blockedAction = action(actionBlockedId, blockedId, RecoveryActionStatus.STOPPED,
                PolicyDecision.DENY, Instant.parse("2026-09-01T00:00:00Z"));
        RecoveryAction awaitingAction = action(actionAwaitingId, awaitingId, RecoveryActionStatus.EXECUTED,
                PolicyDecision.AUTO, Instant.parse("2026-09-01T00:01:00Z"));
        RecoveryOutcome confirmed = mock(RecoveryOutcome.class);
        when(confirmed.isProviderConfirmed()).thenReturn(true);
        when(confirmed.getRecoveryActionId()).thenReturn(actionAwaitingId);
        when(confirmed.getRecoveredAmountMinor()).thenReturn(5_000L);
        when(incidents.findAll()).thenReturn(List.of(blocked, awaiting));
        when(actions.findAll()).thenReturn(List.of(blockedAction, awaitingAction));
        when(outcomes.findAll()).thenReturn(List.of(confirmed));

        LostRevenueExplorer report = new LostRevenueExplorerService(incidents, actions, outcomes).explore();

        assertThat(report.revenueAtRiskMinor()).isEqualTo(30_000L);
        assertThat(report.providerConfirmedRecoveryMinor()).isEqualTo(5_000L);
        assertThat(report.unrecoveredMinor()).isEqualTo(25_000L);
        assertThat(report.reasons()).extracting(LostRevenueExplorer.Reason::category)
                .containsExactlyInAnyOrder("POLICY_OR_GOVERNOR_BLOCKED", "AWAITING_PROVIDER_TRUTH");
        assertThat(report.reasons()).extracting(LostRevenueExplorer.Reason::amountMinor)
                .containsExactlyInAnyOrder(10_000L, 15_000L);
    }

    private RevenueIncident incident(UUID id, long amount) {
        RevenueIncident value = mock(RevenueIncident.class);
        when(value.getIncidentId()).thenReturn(id);
        when(value.getAmountAtRiskMinor()).thenReturn(amount);
        return value;
    }

    private RecoveryAction action(UUID id, UUID incidentId, RecoveryActionStatus status,
                                  PolicyDecision policy, Instant createdAt) {
        RecoveryAction value = mock(RecoveryAction.class);
        when(value.getId()).thenReturn(id);
        when(value.getIncidentId()).thenReturn(incidentId);
        when(value.getStatus()).thenReturn(status);
        when(value.getPolicyDecision()).thenReturn(policy);
        when(value.getCreatedAt()).thenReturn(createdAt);
        return value;
    }
}
