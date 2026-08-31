package com.sentinel.revenue.service;

import com.sentinel.revenue.api.EvidenceCapsuleView;
import com.sentinel.revenue.api.IncidentDetailView;
import com.sentinel.revenue.audit.AuditTrailEntry;
import com.sentinel.revenue.audit.AuditTrailService;
import com.sentinel.revenue.execution.RecoveryTruth;
import com.sentinel.revenue.execution.RecoveryTruthResolver;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class EvidenceCapsuleService {
    private static final int TOTAL_STAGES = 9;
    private final RevenueIncidentRepository incidents;
    private final IncidentFindingRepository findings;
    private final AgentClaimRepository claims;
    private final WebhookEventRepository webhooks;
    private final RecoveryActionRepository actions;
    private final RecoveryOutcomeRepository outcomes;
    private final AuditTrailService auditTrail;
    private final RecoveryTruthResolver truthResolver;

    public EvidenceCapsuleService(RevenueIncidentRepository incidents,
                                  IncidentFindingRepository findings,
                                  AgentClaimRepository claims,
                                  WebhookEventRepository webhooks,
                                  RecoveryActionRepository actions,
                                  RecoveryOutcomeRepository outcomes,
                                  AuditTrailService auditTrail,
                                  RecoveryTruthResolver truthResolver) {
        this.incidents = incidents;
        this.findings = findings;
        this.claims = claims;
        this.webhooks = webhooks;
        this.actions = actions;
        this.outcomes = outcomes;
        this.auditTrail = auditTrail;
        this.truthResolver = truthResolver;
    }

    @Transactional(readOnly = true)
    public EvidenceCapsuleView assemble(UUID incidentId) {
        RevenueIncident incident = incidents.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Revenue incident not found: " + incidentId));
        Instant now = Instant.now();
        RecoveryAction action = actions.findFirstByIncidentIncidentIdOrderByCreatedAtDesc(incidentId).orElse(null);
        RecoveryOutcome outcome = action == null ? null : outcomes.findByRecoveryActionId(action.getId()).orElse(null);
        RecoveryTruth truth = truthResolver.resolve(action, outcome);
        List<IncidentFinding> incidentFindings = findings.findAllByIncidentIncidentId(incidentId);
        List<AgentClaim> incidentClaims = claims.findAllByIncidentIncidentIdOrderByCreatedAtAsc(incidentId);
        List<AuditTrailEntry> audit = auditTrail.trail(incidentId);
        List<EvidenceCapsuleView.WebhookEvidence> webhookViews = webhooks
                .findAllByIncidentIdOrderByReceivedAtAsc(incidentId).stream()
                .map(event -> new EvidenceCapsuleView.WebhookEvidence(event.getEventId(), event.getEventType(),
                        event.isVerified(), event.isProcessed(), event.getReceivedAt(), event.getProcessedAt()))
                .toList();
        List<EvidenceCapsuleView.SystemicEvidence> evidenceViews = incidentFindings.stream()
                .map(finding -> new EvidenceCapsuleView.SystemicEvidence(finding.getId(),
                        finding.getSource().name(), finding.getSummary(), finding.getConfidence(),
                        finding.getCreatedAt(), finding.getValidUntil(),
                        finding.getValidUntil() == null || !finding.getValidUntil().isBefore(now)))
                .toList();
        List<EvidenceCapsuleView.ClaimEvidence> claimViews = incidentClaims.stream()
                .map(claim -> new EvidenceCapsuleView.ClaimEvidence(claim.getId(), claim.getClaimType(),
                        claim.getClaim(), claim.getConfidence(), claim.getEvidenceRefs(),
                        claim.getContradictingEvidenceRefs(), claim.getProposedAction(),
                        claim.getValidationStatus(), claim.getValidationErrors(), claim.getCreatedAt()))
                .toList();
        List<EvidenceCapsuleView.PolicyEvidence> policy = audit.stream()
                .filter(entry -> entry.policyResult() != null || !entry.ruleTrace().isEmpty()
                        || entry.stage().contains("POLICY"))
                .map(entry -> new EvidenceCapsuleView.PolicyEvidence(entry.timestamp(), entry.narrative(),
                        entry.ruleTrace(), entry.policyResult())).toList();
        IncidentDetailView.TruthView truthView = new IncidentDetailView.TruthView(truth.stage(),
                truth.executionMode(), truth.providerAccepted(), truth.awaitingReconciliation(),
                truth.providerConfirmed(), truth.providerConfirmedAmountMinor(), truth.basis());
        EvidenceCapsuleView.ExecutionEvidence execution = action == null ? null
                : new EvidenceCapsuleView.ExecutionEvidence(action.getId(), action.getStatus(),
                action.getPolicyDecision(), action.getExecutionMode(), action.getExternalResourceId(),
                action.getExecutedAt());
        EvidenceCapsuleView.ReconciliationEvidence reconciliation = outcome == null ? null
                : new EvidenceCapsuleView.ReconciliationEvidence(outcome.getId(), outcome.getStatus(),
                outcome.getRecoveredAmountMinor(), outcome.isProviderConfirmed(),
                outcome.getConfirmationSource(), outcome.getSourceEventId(), outcome.getOccurredAt());
        List<String> missing = missingStages(webhookViews, truth, evidenceViews, claimViews,
                incident.getRootCause(), policy, action, outcome);
        String finalOutcome = outcome != null && outcome.isProviderConfirmed()
                ? outcome.getStatus().name() : "NOT_PROVIDER_CONFIRMED";
        return new EvidenceCapsuleView(incidentId, now, webhookViews, truthView, evidenceViews,
                claimViews, incident.getRootCause(), policy, execution, reconciliation, finalOutcome,
                new EvidenceCapsuleView.Completeness(TOTAL_STAGES - missing.size(), TOTAL_STAGES, missing));
    }

    private List<String> missingStages(List<?> webhooks, RecoveryTruth truth, List<?> evidence,
                                       List<?> claims, String prediction, List<?> policy,
                                       RecoveryAction action, RecoveryOutcome outcome) {
        List<String> missing = new ArrayList<>();
        if (webhooks.isEmpty()) missing.add("WEBHOOK");
        if (!truth.providerAccepted() && !truth.providerConfirmed()) missing.add("PROVIDER_TRUTH");
        if (evidence.isEmpty()) missing.add("SYSTEMIC_EVIDENCE");
        if (claims.isEmpty()) missing.add("AGENT_CLAIMS");
        if (prediction == null || prediction.isBlank()) missing.add("PREDICTION");
        if (policy.isEmpty()) missing.add("POLICY");
        if (action == null) missing.add("EXECUTION");
        if (outcome == null) missing.add("RECONCILIATION");
        if (outcome == null || !outcome.isProviderConfirmed()) missing.add("FINAL_OUTCOME");
        return missing;
    }
}
