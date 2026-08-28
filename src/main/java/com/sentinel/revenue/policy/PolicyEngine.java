package com.sentinel.revenue.policy;

import com.sentinel.revenue.detection.RuleOutcome;
import com.sentinel.revenue.model.PolicyDecision;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.RuleListener;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class PolicyEngine {
    private final PolicyProperties properties;
    private final MandatoryStopEvaluator mandatoryStops;

    public PolicyEngine(PolicyProperties properties, MandatoryStopEvaluator mandatoryStops) {
        this.properties = properties;
        this.mandatoryStops = mandatoryStops;
    }

    public PolicyEvaluation evaluate(PolicyContext context) {
        MandatoryStopEvaluator.StopEvaluation stop = mandatoryStops.evaluate(context);
        if (stop.denied()) {
            return new PolicyEvaluation(PolicyDecision.DENY, stop.rules(),
                    "Mandatory stop rule failed; permissive rules were not evaluated.");
        }

        List<PolicyRuleResult> trace = new ArrayList<>(stop.rules());
        Rules rules = allowRules();
        Facts facts = new Facts();
        facts.put(TraceablePolicyRule.CONTEXT, context);
        DefaultRulesEngine engine = new DefaultRulesEngine();
        engine.registerRuleListener(new RuleListener() {
            @Override
            public void afterEvaluate(Rule rule, Facts ignored, boolean evaluationResult) {
                trace.add(((TraceablePolicyRule) rule).result(context, evaluationResult));
            }
        });
        engine.fire(rules, facts);

        Set<String> denyRules = Set.of("PAYMENT_STATUS_SAFE", "NO_ACTIVE_RECOVERY", "STRATEGY_ALLOWED");
        boolean deny = trace.stream().anyMatch(result -> !result.mandatoryStop()
                && result.outcome() == RuleOutcome.FAIL && denyRules.contains(result.rule()));
        boolean allAllow = trace.stream().filter(result -> !result.mandatoryStop())
                .allMatch(result -> result.outcome() == RuleOutcome.PASS);
        PolicyDecision decision = deny ? PolicyDecision.DENY
                : allAllow ? PolicyDecision.AUTO : PolicyDecision.HUMAN;
        String reason = switch (decision) {
            case AUTO -> "All mandatory-stop and autonomous-allow rules passed.";
            case HUMAN -> "No mandatory stop fired, but one or more autonomous thresholds require review.";
            case DENY -> "A deterministic deny gate rejected the proposal.";
        };
        return new PolicyEvaluation(decision, trace, reason);
    }

    private Rules allowRules() {
        Rules rules = new Rules();
        rules.register(
                new TraceablePolicyRule("CONFIDENCE_THRESHOLD", 1,
                        c -> c.confidence() >= properties.autoConfidenceThreshold(),
                        c -> decimal(c.confidence()), ">=", decimal(properties.autoConfidenceThreshold()),
                        "Low-confidence proposals require a human."),
                new TraceablePolicyRule("AMOUNT_THRESHOLD", 2,
                        c -> c.amountMinor() <= properties.maximumAutoAmountMinor(),
                        c -> Long.toString(c.amountMinor()), "<=",
                        Long.toString(properties.maximumAutoAmountMinor()),
                        "High-value proposals require a human."),
                new TraceablePolicyRule("PAYMENT_STATUS_SAFE", 3,
                        this::statusesSafe, c -> c.paymentStatuses().toString(), "excludes",
                        properties.paidOrRefundedStatuses().toString(),
                        "Paid or refunded payments cannot receive recovery actions."),
                new TraceablePolicyRule("NO_ACTIVE_RECOVERY", 4,
                        c -> !c.existingActiveRecovery(),
                        c -> Boolean.toString(c.existingActiveRecovery()), "==", "false",
                        "Only one active recovery may exist for an incident."),
                new TraceablePolicyRule("RETRY_LIMIT", 5,
                        c -> c.retryCount() < properties.maximumAttempts(),
                        c -> Integer.toString(c.retryCount()), "<",
                        Integer.toString(properties.maximumAttempts()),
                        "Excessive retries require human review."),
                new TraceablePolicyRule("PER_CUSTOMER_ACTION_LIMIT", 6,
                        c -> c.maximumCustomerActionCount() < properties.perCustomerActionLimit(),
                        c -> Integer.toString(c.maximumCustomerActionCount()), "<",
                        Integer.toString(properties.perCustomerActionLimit()),
                        "Customer action limits prevent recovery fatigue."),
                new TraceablePolicyRule("STRATEGY_ALLOWED", 7,
                        c -> properties.allowedStrategies().contains(c.strategy()),
                        c -> c.strategy().name(), "in", properties.allowedStrategies().toString(),
                        "Only allowlisted strategies may proceed."));
        return rules;
    }

    private boolean statusesSafe(PolicyContext context) {
        return context.paymentStatuses().stream().map(value -> value.toUpperCase(Locale.ROOT))
                .noneMatch(properties.paidOrRefundedStatuses()::contains);
    }
    private String decimal(double value) { return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }
}
