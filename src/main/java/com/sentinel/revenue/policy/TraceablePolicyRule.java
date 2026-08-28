package com.sentinel.revenue.policy;

import com.sentinel.revenue.detection.RuleOutcome;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.core.BasicRule;

import java.util.function.Function;
import java.util.function.Predicate;

final class TraceablePolicyRule extends BasicRule {
    static final String CONTEXT = "policyContext";
    private final Predicate<PolicyContext> condition;
    private final Function<PolicyContext, String> actual;
    private final String comparison;
    private final String threshold;
    private final String explanation;

    TraceablePolicyRule(String name, int priority, Predicate<PolicyContext> condition,
                        Function<PolicyContext, String> actual, String comparison,
                        String threshold, String explanation) {
        super(name, explanation, priority);
        this.condition = condition;
        this.actual = actual;
        this.comparison = comparison;
        this.threshold = threshold;
        this.explanation = explanation;
    }

    @Override public boolean evaluate(Facts facts) {
        return condition.test(facts.get(CONTEXT));
    }

    @Override public void execute(Facts facts) {
        // A policy rule records a fact; execution permission is decided only after all traces exist.
    }

    PolicyRuleResult result(PolicyContext context, boolean passed) {
        return new PolicyRuleResult(getName(), passed ? RuleOutcome.PASS : RuleOutcome.FAIL,
                actual.apply(context), comparison, threshold, false, explanation);
    }
}
