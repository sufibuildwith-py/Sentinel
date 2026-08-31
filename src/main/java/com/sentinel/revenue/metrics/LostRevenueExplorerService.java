package com.sentinel.revenue.metrics;

import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RecoveryActionStatus;
import com.sentinel.revenue.model.RecoveryOutcome;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.repository.RecoveryActionRepository;
import com.sentinel.revenue.repository.RecoveryOutcomeRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LostRevenueExplorerService {
    private final RevenueIncidentRepository incidents;
    private final RecoveryActionRepository actions;
    private final RecoveryOutcomeRepository outcomes;

    public LostRevenueExplorerService(RevenueIncidentRepository incidents,
                                      RecoveryActionRepository actions,
                                      RecoveryOutcomeRepository outcomes) {
        this.incidents = incidents; this.actions = actions; this.outcomes = outcomes;
    }

    @Transactional(readOnly = true)
    public LostRevenueExplorer explore() {
        List<RevenueIncident> allIncidents = incidents.findAll();
        List<RecoveryAction> allActions = actions.findAll();
        Map<UUID, RecoveryAction> latestAction = new HashMap<>();
        Map<UUID, UUID> incidentByAction = new HashMap<>();
        for (RecoveryAction action : allActions) {
            incidentByAction.put(action.getId(), action.getIncidentId());
            latestAction.merge(action.getIncidentId(), action, (left, right) ->
                    Comparator.comparing(RecoveryAction::getCreatedAt).compare(left, right) >= 0 ? left : right);
        }
        Map<UUID, Long> confirmedByIncident = new HashMap<>();
        for (RecoveryOutcome outcome : outcomes.findAll()) {
            if (!outcome.isProviderConfirmed()) continue;
            UUID incidentId = incidentByAction.get(outcome.getRecoveryActionId());
            if (incidentId != null) confirmedByIncident.merge(incidentId,
                    outcome.getRecoveredAmountMinor(), Long::sum);
        }
        Map<String, MutableReason> reasons = new LinkedHashMap<>();
        long atRisk = 0L;
        long confirmed = 0L;
        for (RevenueIncident incident : allIncidents) {
            atRisk += incident.getAmountAtRiskMinor();
            long incidentConfirmed = Math.min(incident.getAmountAtRiskMinor(),
                    confirmedByIncident.getOrDefault(incident.getIncidentId(), 0L));
            confirmed += incidentConfirmed;
            long remaining = incident.getAmountAtRiskMinor() - incidentConfirmed;
            if (remaining <= 0) continue;
            RecoveryAction action = latestAction.get(incident.getIncidentId());
            String category = category(incident, action);
            reasons.computeIfAbsent(category, ignored -> new MutableReason()).add(remaining);
        }
        List<LostRevenueExplorer.Reason> breakdown = new ArrayList<>();
        reasons.forEach((category, value) -> breakdown.add(new LostRevenueExplorer.Reason(
                category, value.amountMinor, value.incidentCount, evidence(category), explanation(category))));
        breakdown.sort(Comparator.comparingLong(LostRevenueExplorer.Reason::amountMinor).reversed());
        return new LostRevenueExplorer("Lost Revenue Explorer — provider-confirmed truth",
                atRisk, confirmed, Math.max(0, atRisk - confirmed), breakdown,
                "OBSERVED_INCIDENT_AND_PROVIDER_TRUTH",
                List.of("Categories explain current unrecovered state; they do not estimate causal uplift",
                        "Natural recovery remains unavailable until a valid causal baseline exists",
                        "An unresolved amount is never counted as recovered without provider confirmation"));
    }

    private String category(RevenueIncident incident, RecoveryAction action) {
        if (action != null) {
            RecoveryActionStatus status = action.getStatus();
            if (action.getPolicyDecision() == PolicyDecision.DENY
                    || status == RecoveryActionStatus.REJECTED || status == RecoveryActionStatus.STOPPED)
                return "POLICY_OR_GOVERNOR_BLOCKED";
            if (status == RecoveryActionStatus.PENDING_APPROVAL) return "HUMAN_REVIEW_PENDING";
            if (status == RecoveryActionStatus.EXECUTION_UNCERTAIN || status == RecoveryActionStatus.EXECUTED
                    || status == RecoveryActionStatus.PARTIALLY_RECOVERED) return "AWAITING_PROVIDER_TRUTH";
            if (status == RecoveryActionStatus.FAILED || status == RecoveryActionStatus.CANCELLED)
                return "PROVIDER_OR_EXECUTION_FAILED";
        }
        if (incident.getStatus() == RevenueIncidentStatus.HUMAN_REVIEW) return "HUMAN_REVIEW_PENDING";
        if (incident.getStatus() == RevenueIncidentStatus.STOPPED) return "POLICY_OR_GOVERNOR_BLOCKED";
        return "UNRESOLVED_OR_NO_ACTION";
    }

    private String evidence(String category) {
        return switch (category) {
            case "AWAITING_PROVIDER_TRUTH", "PROVIDER_OR_EXECUTION_FAILED" -> "PROVIDER_EXECUTION_STATE";
            case "POLICY_OR_GOVERNOR_BLOCKED" -> "DETERMINISTIC_AUTHORITY_TRACE";
            case "HUMAN_REVIEW_PENDING" -> "PERSISTED_APPROVAL_STATE";
            default -> "OBSERVED_INCIDENT_STATE";
        };
    }

    private String explanation(String category) {
        return switch (category) {
            case "AWAITING_PROVIDER_TRUTH" -> "Provider accepted or uncertain work remains unreconciled.";
            case "PROVIDER_OR_EXECUTION_FAILED" -> "The provider or bounded execution path did not complete.";
            case "POLICY_OR_GOVERNOR_BLOCKED" -> "Deterministic authority deliberately refused the intervention.";
            case "HUMAN_REVIEW_PENDING" -> "Execution is held until persisted human approval.";
            default -> "No currently eligible or completed recovery action explains this open value.";
        };
    }

    private static final class MutableReason {
        private long amountMinor;
        private int incidentCount;
        private void add(long amount) { amountMinor += amount; incidentCount++; }
    }
}
