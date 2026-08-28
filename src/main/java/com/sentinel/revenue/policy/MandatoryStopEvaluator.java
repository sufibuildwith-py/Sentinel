package com.sentinel.revenue.policy;

import com.sentinel.revenue.detection.RuleOutcome;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MandatoryStopEvaluator {
    private final PolicyProperties properties;

    public MandatoryStopEvaluator(PolicyProperties properties) { this.properties = properties; }

    public StopEvaluation evaluate(PolicyContext context) {
        List<PolicyRuleResult> rules = List.of(
                safe("PAYMENT_NOT_ALREADY_RECOVERED", context.paymentAlreadyRecovered(),
                        "false", "Payment already recovered always denies a new action."),
                safe("ACTION_NOT_EXPIRED", !context.actionExpiresAt().isAfter(context.evaluatedAt()),
                        "expires after evaluation time", "Expired proposals cannot be acted on."),
                maximum("MAXIMUM_ATTEMPTS_NOT_REACHED", context.attemptCount(),
                        properties.maximumAttempts() - 1, "Recovery attempts are bounded."),
                maximum("RISK_SCORE_ACCEPTABLE", context.riskScore(),
                        properties.maximumRiskScore(), "Unacceptable risk always denies."),
                safe("NO_DUPLICATE_CHARGE_RISK", context.duplicateChargeRisk(),
                        "false", "Any duplicate-charge signal always denies."));
        boolean denied = rules.stream().anyMatch(rule -> rule.outcome() == RuleOutcome.FAIL);
        return new StopEvaluation(denied, rules);
    }

    private PolicyRuleResult safe(String name, boolean stopCondition, String required, String explanation) {
        return new PolicyRuleResult(name, stopCondition ? RuleOutcome.FAIL : RuleOutcome.PASS,
                Boolean.toString(stopCondition), "==", required, true, explanation);
    }
    private PolicyRuleResult maximum(String name, double actual, double threshold, String explanation) {
        return new PolicyRuleResult(name, actual <= threshold ? RuleOutcome.PASS : RuleOutcome.FAIL,
                Double.toString(actual), "<=", Double.toString(threshold), true, explanation);
    }

    public record StopEvaluation(boolean denied, List<PolicyRuleResult> rules) {
        public StopEvaluation { rules = List.copyOf(rules); }
    }
}
