package com.sentinel.revenue.planning;

import com.sentinel.core.agent.AgentContext;
import com.sentinel.core.agent.AgentResult;
import com.sentinel.revenue.audit.AuditLogService;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.policy.*;
import com.sentinel.revenue.repository.*;
import com.sentinel.revenue.service.RevenueIncidentStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.sentinel.revenue.opportunity.RecoveryOpportunityDecision;
import com.sentinel.revenue.opportunity.RecoveryOpportunityEngine;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class RecoveryPlanningService {
    private static final Set<RecoveryActionStatus> ACTIVE = EnumSet.of(
            RecoveryActionStatus.PROPOSED, RecoveryActionStatus.AUTO_APPROVED,
            RecoveryActionStatus.PENDING_APPROVAL, RecoveryActionStatus.APPROVED,
            RecoveryActionStatus.EXECUTING);

    private final RevenueIncidentRepository incidents;
    private final RecoveryPlanRepository plans;
    private final RecoveryActionRepository actions;
    private final RecoveryOutcomeRepository outcomes;
    private final PaymentEventRepository payments;
    private final RecoveryPlannerAgent planner;
    private final PolicyEngine policyEngine;
    private final PolicyProperties properties;
    private final AuditLogService audit;
    private final RecoveryOpportunityEngine opportunities;
    private final RevenueIncidentStateMachine stateMachine = new RevenueIncidentStateMachine();

    @Autowired
    public RecoveryPlanningService(RevenueIncidentRepository incidents, RecoveryPlanRepository plans,
                                   RecoveryActionRepository actions, RecoveryOutcomeRepository outcomes,
                                   PaymentEventRepository payments, RecoveryPlannerAgent planner,
                                   PolicyEngine policyEngine, PolicyProperties properties,
                                   AuditLogService audit, RecoveryOpportunityEngine opportunities) {
        this.incidents = incidents;
        this.plans = plans;
        this.actions = actions;
        this.outcomes = outcomes;
        this.payments = payments;
        this.planner = planner;
        this.policyEngine = policyEngine;
        this.properties = properties;
        this.audit = audit;
        this.opportunities = opportunities;
    }

    public RecoveryPlanningService(RevenueIncidentRepository incidents, RecoveryPlanRepository plans,
                                   RecoveryActionRepository actions, RecoveryOutcomeRepository outcomes,
                                   PaymentEventRepository payments, RecoveryPlannerAgent planner,
                                   PolicyEngine policyEngine, PolicyProperties properties,
                                   AuditLogService audit) {
        this(incidents, plans, actions, outcomes, payments, planner, policyEngine, properties, audit, null);
    }

    @Transactional
    public RecoveryPlanningResult plan(UUID incidentId) {
        RevenueIncident incident = incidents.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Revenue incident not found: " + incidentId));
        if (incident.getStatus() != RevenueIncidentStatus.DIAGNOSED) {
            throw new IllegalStateException("Only DIAGNOSED incidents can enter recovery planning");
        }
        transition(incident, RevenueIncidentStatus.PLANNING, "Recovery planning started");
        Instant now = Instant.now();
        if (opportunities != null) {
            RecoveryOpportunityDecision shadow = opportunities.evaluate(incident, null);
            audit.append(incident, "OPPORTUNITY_ENGINE", null, "SHADOW_OPPORTUNITY_EVALUATED",
                    shadow.candidates().stream().map(candidate -> candidate.action() + ":" + candidate.estimateKind()).toList(),
                    null, "Shadow choice " + shadow.shadowChoice(), List.of(), shadow.mode(), null,
                    null, "Fallback planner retains authority at maturity " + shadow.maturity());
        }
        AgentContext context = new AgentContext(incidentId.toString(), now,
                now.plus(2, ChronoUnit.MINUTES), Map.of("permission", "PROPOSAL_ONLY"));
        AgentResult<RecoveryPlan> agentResult = planner.execute(incident, context);
        RecoveryPlan plan = plans.saveAndFlush(agentResult.output());
        audit.append(incident, "SENTINEL", agentResult.agentName(), "AGENT_RESULT",
                agentResult.evidence().stream().map(com.sentinel.core.agent.Evidence::description).toList(),
                decimal(agentResult.confidence().value()), agentResult.summary(), List.of(), null,
                null, null, "Proposal only; no execution permission granted");
        audit.append(incident, "SENTINEL", null, "RECOVERY_PROPOSED", List.of(plan.getReason()),
                plan.getConfidence(), "Proposed " + plan.getStrategy(), List.of(), null,
                null, null, "Estimated recovery minor=" + plan.getEstimatedRecoveryMinor());

        transition(incident, RevenueIncidentStatus.POLICY_REVIEW, "Deterministic policy review started");
        PolicyEvaluation policy = policyEngine.evaluate(contextFor(incident, plan, now));
        for (PolicyRuleResult rule : policy.rules()) {
            audit.append(incident, "POLICY_ENGINE", null, "POLICY_RULE_EVALUATED",
                    List.of(rule.evidenceLine()), null, rule.explanation(),
                    List.of(rule.evidenceLine()), null, null, null,
                    rule.outcome().name());
        }
        audit.append(incident, "POLICY_ENGINE", null, "POLICY_DECISION",
                policy.rules().stream().map(PolicyRuleResult::evidenceLine).toList(),
                plan.getConfidence(), policy.reason(),
                policy.rules().stream().map(PolicyRuleResult::evidenceLine).toList(),
                policy.decision().name(), null, null, policy.decision().name());

        // The audit decision is flushed before an action object can be constructed or persisted.
        RecoveryAction action = RecoveryAction.fromPersistedPolicy(plan, incident, policy,
                plan.getTargetAmountMinor(), now);
        action = actions.saveAndFlush(action);
        RevenueIncidentStatus finalStatus = switch (policy.decision()) {
            case AUTO -> RevenueIncidentStatus.APPROVED;
            case HUMAN -> RevenueIncidentStatus.HUMAN_REVIEW;
            case DENY -> RevenueIncidentStatus.STOPPED;
        };
        transition(incident, finalStatus, "Policy decision " + policy.decision());
        audit.append(incident, "SENTINEL", null, "RECOVERY_ACTION_CREATED", List.of(plan.getReason()),
                plan.getConfidence(), "Action status " + action.getStatus(),
                policy.rules().stream().map(PolicyRuleResult::evidenceLine).toList(),
                policy.decision().name(), null, null,
                "Action " + action.getId() + " created without execution");
        return new RecoveryPlanningResult(incidentId, plan.getId(), action.getId(), plan.getStrategy(),
                policy.decision(), action.getStatus(), incident.getStatus(), policy.rules(), policy.reason());
    }

    private PolicyContext contextFor(RevenueIncident incident, RecoveryPlan plan, Instant now) {
        List<PaymentEvent> events = payments.findAllByPaymentIdIn(incident.getAffectedPayments());
        Set<String> statuses = events.stream().map(PaymentEvent::getStatus)
                .filter(Objects::nonNull).map(String::toUpperCase).collect(java.util.stream.Collectors.toSet());
        boolean recovered = outcomes.findAllByIncidentIncidentId(incident.getIncidentId()).stream()
                .anyMatch(outcome -> outcome.getStatus() == RecoveryOutcomeStatus.RECOVERED);
        int attempts = events.stream().mapToInt(PaymentEvent::getAttemptNumber).max().orElse(1);
        int retries = events.stream().mapToInt(event -> Math.max(event.getAttemptNumber() - 1,
                event.getPreviousFailureCount())).max().orElse(0);
        double risk = events.stream().map(PaymentEvent::getMetadata).map(map -> map.get("riskScore"))
                .filter(Number.class::isInstance).map(Number.class::cast).mapToDouble(Number::doubleValue)
                .max().orElse(0.0);
        boolean duplicateRisk = incident.getEvidence().stream().anyMatch(this::duplicateSignal)
                || events.stream().map(PaymentEvent::getMetadata)
                .anyMatch(map -> Boolean.TRUE.equals(map.get("duplicateChargeRisk")));
        boolean paidStatus = statuses.stream().anyMatch(properties.paidOrRefundedStatuses()::contains);
        boolean active = !actions.findAllByIncidentIncidentIdAndStatusIn(
                incident.getIncidentId(), ACTIVE).isEmpty();
        int customerLimit = maximumCustomerActionCount(incident);
        return new PolicyContext(plan.getConfidence().doubleValue(), plan.getTargetAmountMinor(), statuses,
                active, retries, customerLimit, plan.getStrategy(), recovered || paidStatus,
                now.plus(properties.actionTtl()), now, attempts, risk, duplicateRisk);
    }

    private int maximumCustomerActionCount(RevenueIncident current) {
        Map<String, Integer> counts = new HashMap<>();
        for (RecoveryAction action : actions.findAll()) {
            incidents.findById(action.getIncidentId()).ifPresent(other -> other.getAffectedCustomers()
                    .forEach(customer -> counts.merge(customer, 1, Integer::sum)));
        }
        return current.getAffectedCustomers().stream().mapToInt(customer -> counts.getOrDefault(customer, 0))
                .max().orElse(0);
    }

    private boolean duplicateSignal(String evidence) {
        String normalized = evidence.toUpperCase();
        return normalized.contains("DUPLICATE_CHARGE") || normalized.contains("DUPLICATE CHARGE");
    }

    private void transition(RevenueIncident incident, RevenueIncidentStatus target, String outcome) {
        RevenueIncidentStatus previous = incident.getStatus();
        incident.transitionTo(stateMachine.transition(previous, target));
        incidents.saveAndFlush(incident);
        audit.append(incident, "SENTINEL", null, "STATE_TRANSITION", List.of(), null,
                outcome, List.of(), null, previous, target, outcome);
    }

    private BigDecimal decimal(double value) { return BigDecimal.valueOf(value).setScale(4, java.math.RoundingMode.HALF_UP); }
}
