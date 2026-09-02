package com.sentinel.revenue.service;

import com.sentinel.revenue.audit.AuditTrailService;
import com.sentinel.revenue.execution.RecoveryExecutionEligibilityEvaluator;
import com.sentinel.revenue.execution.RecoveryTruthResolver;
import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RecoveryActionStatus;
import com.sentinel.revenue.model.RecoveryPlan;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.repository.IncidentFindingRepository;
import com.sentinel.revenue.repository.RecoveryActionRepository;
import com.sentinel.revenue.repository.RecoveryGovernorDecisionRepository;
import com.sentinel.revenue.repository.RecoveryOutcomeRepository;
import com.sentinel.revenue.repository.RecoveryPlanRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevenueOperationsReadServiceTest {

    @Test
    void approvalsOnlyReadsActionsWhoseIncidentsRemainOperational() {
        RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
        when(actions.findAllOperational()).thenReturn(List.of());
        RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        RevenueOperationsReadService service = service(incidents, actions, mock(RecoveryPlanRepository.class));

        assertThat(service.approvals()).isEmpty();
        verify(actions).findAllOperational();
        verify(actions, never()).findAll();
    }

    @Test
    void livePendingApprovalRetainsItsIncidentScopedContext() {
        UUID incidentId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID actionId = UUID.randomUUID();
        RecoveryAction action = mock(RecoveryAction.class);
        when(action.getId()).thenReturn(actionId);
        when(action.getIncidentId()).thenReturn(incidentId);
        when(action.getRecoveryPlanId()).thenReturn(planId);
        when(action.getStatus()).thenReturn(RecoveryActionStatus.PENDING_APPROVAL);
        when(action.getAmountMinor()).thenReturn(25_000L);
        RevenueIncident incident = mock(RevenueIncident.class);
        when(incident.getIncidentId()).thenReturn(incidentId);
        when(incident.getType()).thenReturn("UPI_DEGRADATION");
        RecoveryPlan plan = mock(RecoveryPlan.class);
        when(plan.getConfidence()).thenReturn(new BigDecimal("0.8200"));
        when(plan.getReason()).thenReturn("Human review required");
        RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
        when(actions.findAllOperational()).thenReturn(List.of(action));
        RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        when(incidents.findById(incidentId)).thenReturn(Optional.of(incident));
        RecoveryPlanRepository plans = mock(RecoveryPlanRepository.class);
        when(plans.findById(planId)).thenReturn(Optional.of(plan));

        assertThat(service(incidents, actions, plans).approvals()).singleElement().satisfies(item -> {
            assertThat(item.incidentId()).isEqualTo(incidentId);
            assertThat(item.actionId()).isEqualTo(actionId);
            assertThat(item.amountMinor()).isEqualTo(25_000L);
        });
    }

    private RevenueOperationsReadService service(RevenueIncidentRepository incidents,
                                                  RecoveryActionRepository actions,
                                                  RecoveryPlanRepository plans) {
        return new RevenueOperationsReadService(incidents, plans, actions,
                mock(RecoveryOutcomeRepository.class), mock(IncidentFindingRepository.class),
                mock(RecoveryGovernorDecisionRepository.class), mock(AuditTrailService.class),
                mock(RecoveryTruthResolver.class), mock(RecoveryExecutionEligibilityEvaluator.class));
    }
}
