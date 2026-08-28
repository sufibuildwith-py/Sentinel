package com.sentinel.revenue.service;

import com.sentinel.revenue.api.HumanDecisionRequest;
import com.sentinel.revenue.audit.AuditLogService;
import com.sentinel.revenue.detection.RuleOutcome;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.policy.PolicyEvaluation;
import com.sentinel.revenue.policy.PolicyRuleResult;
import com.sentinel.revenue.repository.RecoveryActionRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HumanApprovalServiceTest {
    @Test
    void approvalPersistsActorReasonAndStopsBeforeExecution() {
        Fixture fixture = fixture();
        HumanDecisionRequest request = new HumanDecisionRequest("reviewer@example", "Verified customer intent");

        var response = fixture.service.approve(fixture.actionId, request);

        assertThat(response.actionStatus()).isEqualTo(RecoveryActionStatus.APPROVED);
        assertThat(response.incidentStatus()).isEqualTo(RevenueIncidentStatus.APPROVED);
        assertThat(fixture.action.getStatus()).isNotEqualTo(RecoveryActionStatus.EXECUTING);
        verify(fixture.audit, atLeastOnce()).append(eq(fixture.incident), eq("reviewer@example"),
                nullable(String.class), anyString(), anyList(), nullable(BigDecimal.class),
                anyString(), anyList(), nullable(String.class), nullable(RevenueIncidentStatus.class),
                nullable(RevenueIncidentStatus.class), anyString());
    }

    @Test
    void rejectionPersistsReasonAndStopsActionAndIncident() {
        Fixture fixture = fixture();
        var response = fixture.service.reject(fixture.actionId,
                new HumanDecisionRequest("risk-officer", "Possible duplicate payment"));
        assertThat(response.actionStatus()).isEqualTo(RecoveryActionStatus.REJECTED);
        assertThat(response.incidentStatus()).isEqualTo(RevenueIncidentStatus.STOPPED);
    }

    private Fixture fixture() {
        RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
        RevenueIncidentRepository incidents = mock(RevenueIncidentRepository.class);
        AuditLogService audit = mock(AuditLogService.class);
        RevenueIncident incident = new RevenueIncident("UPI_DEGRADATION", RevenueIncidentStatus.HUMAN_REVIEW,
                "HIGH", 200_000, Instant.now(), List.of("p1"), List.of("c1"), List.of(), null, null);
        UUID incidentId = UUID.randomUUID();
        ReflectionTestUtils.setField(incident, "incidentId", incidentId);
        RecoveryPlan plan = new RecoveryPlan(incident, RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK,
                "proposal", 1, 200_000, new BigDecimal("0.7000"), 100_000,
                RiskLevel.MEDIUM, Instant.now());
        RecoveryAction action = RecoveryAction.fromPersistedPolicy(plan, incident,
                new PolicyEvaluation(PolicyDecision.HUMAN, List.of(new PolicyRuleResult(
                        "CONFIDENCE_THRESHOLD", RuleOutcome.FAIL, "0.7", ">=", "0.85", false,
                        "human required")), "human required"), 200_000, Instant.now());
        UUID actionId = UUID.randomUUID();
        ReflectionTestUtils.setField(action, "id", actionId);
        when(actions.findById(actionId)).thenReturn(Optional.of(action));
        when(incidents.findById(incidentId)).thenReturn(Optional.of(incident));
        return new Fixture(new HumanApprovalService(actions, incidents, audit), action, incident,
                actionId, audit);
    }

    private record Fixture(HumanApprovalService service, RecoveryAction action,
                           RevenueIncident incident, UUID actionId, AuditLogService audit) {}
}
