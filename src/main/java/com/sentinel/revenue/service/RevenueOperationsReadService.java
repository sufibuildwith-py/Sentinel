package com.sentinel.revenue.service;

import com.sentinel.revenue.api.*;
import com.sentinel.revenue.audit.AuditTrailEntry;
import com.sentinel.revenue.audit.AuditTrailService;
import com.sentinel.revenue.execution.RecoveryTruth;
import com.sentinel.revenue.execution.RecoveryTruthResolver;
import com.sentinel.revenue.execution.RecoveryExecutionEligibility;
import com.sentinel.revenue.execution.RecoveryExecutionEligibilityEvaluator;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RevenueOperationsReadService {
    private final RevenueIncidentRepository incidents;
    private final RecoveryPlanRepository plans;
    private final RecoveryActionRepository actions;
    private final RecoveryOutcomeRepository outcomes;
    private final IncidentFindingRepository findings;
    private final RecoveryGovernorDecisionRepository governorDecisions;
    private final AuditTrailService audit;
    private final RecoveryTruthResolver truthResolver;
    private final RecoveryExecutionEligibilityEvaluator executionEligibility;
    public RevenueOperationsReadService(RevenueIncidentRepository incidents, RecoveryPlanRepository plans,
                                        RecoveryActionRepository actions, RecoveryOutcomeRepository outcomes,
                                        IncidentFindingRepository findings,
                                        RecoveryGovernorDecisionRepository governorDecisions,
                                        AuditTrailService audit,
                                        RecoveryTruthResolver truthResolver,
                                        RecoveryExecutionEligibilityEvaluator executionEligibility) {
        this.incidents = incidents; this.plans = plans; this.actions = actions;
        this.outcomes = outcomes; this.findings = findings; this.audit = audit;
        this.governorDecisions = governorDecisions;
        this.truthResolver = truthResolver;
        this.executionEligibility = executionEligibility;
    }

    @Transactional(readOnly = true)
    public List<IncidentSummaryView> incidents() {
        return incidents.findAll().stream().sorted(Comparator.comparing(RevenueIncident::getDetectedAt).reversed())
                .map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public IncidentDetailView incident(UUID id) {
        RevenueIncident incident = incidents.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Revenue incident not found: " + id));
        RecoveryPlan plan = latestPlan(id).orElse(null);
        RecoveryAction action = latestAction(id).orElse(null);
        RecoveryOutcome outcome = action == null ? null
                : outcomes.findByRecoveryActionId(action.getId()).orElse(null);
        List<IncidentDetailView.FindingView> safeFindings = findings.findAllByIncidentIncidentId(id).stream()
                .map(finding -> new IncidentDetailView.FindingView(finding.getSource().name(),
                        finding.getSummary(), finding.getConfidence(), finding.getEvidence(), finding.getCreatedAt()))
                .toList();
        IncidentDetailView.PlanView planView = plan == null ? null : new IncidentDetailView.PlanView(
                plan.getId(), plan.getStrategy(), plan.getReason(), plan.getTargetAmountMinor(),
                plan.getConfidence(), plan.getRiskLevel());
        IncidentDetailView.ActionView actionView = action == null ? null : new IncidentDetailView.ActionView(
                action.getId(), action.getStatus(), action.getPolicyDecision(), action.getAmountMinor(),
                action.getCurrency(), action.getExternalResourceId(), action.getProviderReferenceId(),
                action.getExternalResourceUrl(), action.getExternalResourceStatus(), action.getExecutionAttempts(),
                action.getApprovedAt(), action.getExecutedAt());
        RecoveryGovernorDecision governorDecision = governorDecisions
                .findAllByIncidentIdOrderByCreatedAtAsc(id).stream()
                .filter(decision -> action == null || action.getId().equals(decision.getRecoveryActionId()))
                .reduce((first, second) -> second).orElse(null);
        IncidentDetailView.GovernorView governorView = governorDecision == null ? null
                : new IncidentDetailView.GovernorView(governorDecision.getId(), governorDecision.isAllowed(),
                governorDecision.getAllowedValueMinor(), governorDecision.getViolations(),
                governorDecision.getCreatedAt());
        RecoveryTruth truth = truthResolver.resolve(action, outcome);
        IncidentDetailView.TruthView truthView = new IncidentDetailView.TruthView(
                truth.stage(), truth.executionMode(), truth.providerAccepted(),
                truth.awaitingReconciliation(), truth.providerConfirmed(),
                truth.providerConfirmedAmountMinor(), truth.basis());
        RecoveryExecutionEligibility availability = executionEligibility.evaluate(action, plan, governorDecision);
        IncidentDetailView.ExecutionAvailabilityView availabilityView =
                new IncidentDetailView.ExecutionAvailabilityView(availability.enabled(), availability.eligible(),
                        availability.reasonCode(), availability.reason());
        return new IncidentDetailView(summary(incident), safeFindings, planView, actionView,
                governorView, truthView, availabilityView);
    }

    @Transactional(readOnly = true)
    public List<ApprovalQueueItem> approvals() {
        return actions.findAllOperational().stream()
                .filter(action -> action.getStatus() == RecoveryActionStatus.PENDING_APPROVAL)
                .map(action -> {
                    RevenueIncident incident = incidents.findById(action.getIncidentId()).orElseThrow();
                    RecoveryPlan plan = plans.findById(action.getRecoveryPlanId()).orElseThrow();
                    List<String> failed = audit.trail(incident.getIncidentId()).stream()
                            .filter(entry -> "POLICY_RULE_EVALUATED".equals(entry.stage()))
                            .flatMap(entry -> entry.evidence().stream())
                            .filter(line -> line.contains("FAIL")).toList();
                    return new ApprovalQueueItem(action.getId(), incident.getIncidentId(), incident.getType(),
                            action.getAmountMinor(), plan.getConfidence(), plan.getReason(), failed);
                }).toList();
    }

    private IncidentSummaryView summary(RevenueIncident incident) {
        RecoveryPlan plan = latestPlan(incident.getIncidentId()).orElse(null);
        RecoveryAction action = latestAction(incident.getIncidentId()).orElse(null);
        RecoveryOutcome outcome = action == null ? null
                : outcomes.findByRecoveryActionId(action.getId()).orElse(null);
        return new IncidentSummaryView(incident.getIncidentId(), incident.getType(), incident.getStatus(),
                incident.getSeverity(), incident.getAmountAtRiskMinor(), incident.getDetectedAt(),
                incident.getAffectedPayments().size(), incident.getAffectedCustomers().size(),
                plan == null ? null : plan.getStrategy(), action == null ? null : action.getPolicyDecision(),
                action == null ? null : action.getStatus(), outcome == null ? null : outcome.getStatus(),
                outcome == null ? 0 : outcome.getRecoveredAmountMinor());
    }
    private Optional<RecoveryPlan> latestPlan(UUID incidentId) {
        return plans.findAllByIncidentIncidentIdOrderByCreatedAtDesc(incidentId).stream().findFirst();
    }
    private Optional<RecoveryAction> latestAction(UUID incidentId) {
        return actions.findAllByIncidentIncidentId(incidentId).stream()
                .max(Comparator.comparing(RecoveryAction::getCreatedAt));
    }
}
