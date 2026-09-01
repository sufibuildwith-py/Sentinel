package com.sentinel.revenue.governor;

import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class RecoverySafetyGovernor {
    private final RecoverySafetyProperties properties;
    private final KillSwitchService killSwitches;
    private final RecoveryActionRepository actions;
    private final RecoveryOutcomeRepository outcomes;
    private final RecoveryJobRepository jobs;
    private final RecoveryGovernorDecisionRepository decisions;
    private final DynamicRecoveryGovernor dynamicGovernor;
    public RecoverySafetyGovernor(RecoverySafetyProperties properties, KillSwitchService killSwitches,
                                  RecoveryActionRepository actions, RecoveryOutcomeRepository outcomes,
                                  RecoveryJobRepository jobs, RecoveryGovernorDecisionRepository decisions,
                                  DynamicRecoveryGovernor dynamicGovernor) {
        this.properties = properties; this.killSwitches = killSwitches; this.actions = actions;
        this.outcomes = outcomes; this.jobs = jobs; this.decisions = decisions;
        this.dynamicGovernor = dynamicGovernor;
    }

    @Transactional
    public GovernorEvaluation evaluate(RecoveryAction action, RecoveryStrategy strategy,
                                       long requestedValueMinor, Instant now) {
        ExecutionEnvelope envelope = envelope();
        List<String> violations = new ArrayList<>();
        if (action.getPolicyDecision() == PolicyDecision.AUTO
                && killSwitches.enabled(KillSwitch.ALL_AUTONOMOUS_EXECUTION))
            violations.add("KILL_SWITCH:ALL_AUTONOMOUS_EXECUTION");
        if (strategy == RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK
                && killSwitches.enabled(KillSwitch.PAYMENT_LINK_CREATION))
            violations.add("KILL_SWITCH:PAYMENT_LINK_CREATION");
        if (requestedValueMinor > envelope.maxValuePerIncidentMinor())
            violations.add("MAX_VALUE_PER_INCIDENT actual=" + requestedValueMinor
                    + " limit=" + envelope.maxValuePerIncidentMinor());
        List<RecoveryAction> allActions = actions.findAllOperational();
        long totalValue = allActions.stream().filter(this::active).mapToLong(RecoveryAction::getAmountMinor).sum();
        if (totalValue + requestedValueMinor > envelope.maxTotalValueMinor())
            violations.add("MAX_TOTAL_VALUE actual=" + (totalValue + requestedValueMinor)
                    + " limit=" + envelope.maxTotalValueMinor());
        long activeIncidents = allActions.stream().filter(this::active).map(RecoveryAction::getIncidentId).distinct().count();
        if (activeIncidents >= envelope.maxIncidents()) violations.add("MAX_INCIDENTS limit=" + envelope.maxIncidents());
        Instant evaluatedAt = now == null ? Instant.now() : now;
        long recentProviderCalls = allActions.stream().map(RecoveryAction::getExecutionClaimedAt)
                .filter(Objects::nonNull).filter(at -> !at.isBefore(evaluatedAt.minusSeconds(60))).count();
        if (recentProviderCalls >= envelope.maxProviderCallsPerMinute())
            violations.add("MAX_PROVIDER_CALLS_PER_MINUTE actual=" + recentProviderCalls);
        if (action.getExecutionAttempts() >= envelope.maxRetryCount())
            violations.add("MAX_RETRY_COUNT actual=" + action.getExecutionAttempts());
        List<RecoveryJob> allJobs = jobs.findAllOperational();
        long concurrent = allJobs.stream().filter(job -> RecoveryJob.RUNNING.equals(job.getStatus())).count();
        if (concurrent >= envelope.maxConcurrentJobs()) violations.add("MAX_CONCURRENT_JOBS actual=" + concurrent);
        double toolFailureRate = toolFailureRate(allJobs);
        if (toolFailureRate > envelope.maxToolFailureRate())
            violations.add("MAX_TOOL_FAILURE_RATE actual=" + toolFailureRate);
        long unreconciled = allActions.stream().filter(this::awaitingReconciliation)
                .filter(candidate -> outcomes.findByRecoveryActionId(candidate.getId()).stream()
                        .noneMatch(RecoveryOutcome::isProviderConfirmed))
                .mapToLong(RecoveryAction::getAmountMinor).sum();
        DynamicGovernorAssessment posture = dynamicGovernor.assess(new GovernorSignalSnapshot(
                toolFailureRate, unreconciled, totalValue, envelope.maxUnreconciledValueMinor(),
                envelope.maxTotalValueMinor()), envelope.maxToolFailureRate());
        if (posture.posture() == GovernorPosture.RED) violations.add("DYNAMIC_GOVERNOR_RED");
        if (posture.posture() == GovernorPosture.ORANGE && action.getPolicyDecision() == PolicyDecision.AUTO)
            violations.add("DYNAMIC_GOVERNOR_ORANGE_HUMAN_REQUIRED");
        if (posture.posture() == GovernorPosture.YELLOW) {
            long canaryLimit = Math.max(1, Math.round(envelope.maxValuePerIncidentMinor()
                    * posture.authorityMultiplier()));
            if (requestedValueMinor > canaryLimit)
                violations.add("DYNAMIC_GOVERNOR_YELLOW_CANARY_LIMIT actual=" + requestedValueMinor
                        + " limit=" + canaryLimit);
        }
        if (unreconciled + requestedValueMinor > envelope.maxUnreconciledValueMinor())
            violations.add("MAX_UNRECONCILED_VALUE actual=" + (unreconciled + requestedValueMinor));
        boolean allowed = violations.isEmpty();
        RecoveryGovernorDecision persisted = decisions.saveAndFlush(new RecoveryGovernorDecision(
                action.getIncidentId(), action.getId(), allowed, allowed ? requestedValueMinor : 0,
                envelope, violations, evaluatedAt));
        return new GovernorEvaluation(persisted.getId(), allowed, allowed ? requestedValueMinor : 0,
                envelope, violations);
    }

    private ExecutionEnvelope envelope() { return new ExecutionEnvelope(properties.maxTotalValueMinor(),
            properties.maxIncidents(), properties.maxValuePerIncidentMinor(),
            properties.maxProviderCallsPerMinute(), properties.maxCustomerContacts(),
            properties.maxRetryCount(), properties.maxConcurrentJobs(),
            properties.maxToolFailureRate(), properties.maxUnreconciledValueMinor()); }
    private boolean active(RecoveryAction action) { return switch (action.getStatus()) {
        case AUTO_APPROVED, APPROVED, EXECUTING, RETRY_PENDING, EXECUTION_UNCERTAIN, EXECUTED, PARTIALLY_RECOVERED -> true;
        default -> false;
    }; }
    private boolean awaitingReconciliation(RecoveryAction action) { return action.getStatus() == RecoveryActionStatus.EXECUTED
            || action.getStatus() == RecoveryActionStatus.PARTIALLY_RECOVERED
            || action.getStatus() == RecoveryActionStatus.EXECUTION_UNCERTAIN; }
    private double toolFailureRate(List<RecoveryJob> allJobs) {
        if (allJobs.isEmpty()) return 0;
        long failures = allJobs.stream().filter(job -> RecoveryJob.EXHAUSTED.equals(job.getStatus())
                || job.getErrorDetail() != null).count();
        return (double) failures / allJobs.size();
    }
}
