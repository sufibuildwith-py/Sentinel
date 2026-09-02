package com.sentinel.revenue.execution;

import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RecoveryActionStatus;
import com.sentinel.revenue.model.RecoveryGovernorDecision;
import com.sentinel.revenue.model.RecoveryPlan;
import com.sentinel.revenue.model.RecoveryStrategy;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RecoveryExecutionEligibilityEvaluator {
    private static final Set<RecoveryActionStatus> SUBMITTED = Set.of(
            RecoveryActionStatus.EXECUTING, RecoveryActionStatus.EXECUTED,
            RecoveryActionStatus.PARTIALLY_RECOVERED, RecoveryActionStatus.RECOVERED);
    private static final Set<RecoveryActionStatus> RESUMABLE = Set.of(
            RecoveryActionStatus.RETRY_PENDING, RecoveryActionStatus.EXECUTION_UNCERTAIN);

    private final RazorpayProperties properties;

    public RecoveryExecutionEligibilityEvaluator(RazorpayProperties properties) {
        this.properties = properties;
    }

    public RecoveryExecutionEligibility evaluate(RecoveryAction action, RecoveryPlan plan,
                                                   RecoveryGovernorDecision governor) {
        if (action == null || plan == null) {
            return denied("RECOVERY_DECISION_INCOMPLETE", "A persisted recovery plan and action are required");
        }
        if (SUBMITTED.contains(action.getStatus()) || action.getExternalResourceId() != null) {
            return denied("ACTION_ALREADY_SUBMITTED", "A provider action has already been claimed or submitted");
        }
        if (governor != null && !governor.isAllowed()) {
            return denied("GOVERNOR_DENIED", "The persisted recovery governor decision blocks execution");
        }
        if (plan.getStrategy() != RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK) {
            return denied("UNSUPPORTED_RECOVERY_STRATEGY",
                    "Only ALTERNATIVE_PAYMENT_LINK is executable through the Test Mode provider path");
        }
        boolean auto = action.getStatus() == RecoveryActionStatus.AUTO_APPROVED
                && action.getPolicyDecision() == PolicyDecision.AUTO;
        boolean human = action.getStatus() == RecoveryActionStatus.APPROVED
                && action.getPolicyDecision() == PolicyDecision.HUMAN && action.getApprovedAt() != null;
        if (!auto && !human && !RESUMABLE.contains(action.getStatus())) {
            return denied("EXECUTION_PERMISSION_REQUIRED",
                    "Persisted policy and human-approval state do not grant execution permission");
        }
        if (!properties.enabled()) {
            return new RecoveryExecutionEligibility(false, false, "RAZORPAY_EXECUTION_DISABLED",
                    "Razorpay Test Mode execution is disabled in this backend deployment");
        }
        return new RecoveryExecutionEligibility(true, true, "ELIGIBLE",
                "Persisted policy and approval state permit governor evaluation and one idempotent provider action");
    }

    private RecoveryExecutionEligibility denied(String code, String reason) {
        return new RecoveryExecutionEligibility(properties.enabled(), false, code, reason);
    }
}
