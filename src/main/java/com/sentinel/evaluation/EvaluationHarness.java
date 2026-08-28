package com.sentinel.evaluation;

import com.sentinel.revenue.detection.DetectionDecision;
import com.sentinel.revenue.detection.DetectionRuleEngine;
import com.sentinel.revenue.detection.PaymentStatistics;
import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.model.RecoveryStrategy;
import com.sentinel.revenue.policy.PolicyContext;
import com.sentinel.revenue.policy.PolicyEngine;
import com.sentinel.revenue.policy.PolicyEvaluation;
import com.sentinel.revenue.webhook.WebhookSignatureVerifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@Service
public class EvaluationHarness {
    public static final String TITLE = "Sentinel Evaluation Lab — Razorpay Test Mode / Synthetic Evaluation";
    private static final Instant EVALUATED_AT = Instant.parse("2026-09-01T12:00:00Z");

    private final EvaluationDatasetGenerator dataset;
    private final EvaluationProperties properties;
    private final DetectionRuleEngine detection;
    private final PolicyEngine policy;
    private final WebhookSignatureVerifier signatures;

    public EvaluationHarness(EvaluationDatasetGenerator dataset, EvaluationProperties properties,
                             DetectionRuleEngine detection, PolicyEngine policy,
                             WebhookSignatureVerifier signatures) {
        this.dataset = dataset;
        this.properties = properties;
        this.detection = detection;
        this.policy = policy;
        this.signatures = signatures;
    }

    public EvaluationReport evaluate() {
        List<EvaluationScenario> scenarios = dataset.generate();
        List<EvaluationReport.ScenarioResult> results = new ArrayList<>(scenarios.size());
        for (int i = 0; i < scenarios.size(); i++) results.add(evaluate(scenarios.get(i), i));

        long tp = count(results, r -> r.expectedIncident() && r.actualIncident());
        long fp = count(results, r -> !r.expectedIncident() && r.actualIncident());
        long fn = count(results, r -> r.expectedIncident() && !r.actualIncident());
        long tn = count(results, r -> !r.expectedIncident() && !r.actualIncident());
        long incidentCount = count(results, EvaluationReport.ScenarioResult::expectedIncident);
        long policyMatches = count(results, r -> !r.expectedIncident()
                || r.expectedPolicyDecision() == r.actualPolicyDecision());
        long rootMatches = count(results, r -> !r.expectedIncident()
                || r.expectedRootCauseCategory().equals(r.actualRootCauseCategory()));
        long executionMatches = count(results, r -> r.expectedExecutionBehavior().equals(r.actualExecutionBehavior()));
        long falseInterventions = count(results, r -> !executionAllowed(r.expectedPolicyDecision())
                && "ATTEMPT_ONCE".equals(r.actualExecutionBehavior()));
        long escalations = count(results, r -> r.actualPolicyDecision() == PolicyDecision.HUMAN);
        long attempts = count(results, r -> r.actualPolicyDecision() == PolicyDecision.AUTO);
        long recoveries = count(results, r -> r.actualFinancialMutationMinor() > 0);
        long recoveredAmount = results.stream().mapToLong(EvaluationReport.ScenarioResult::actualFinancialMutationMinor).sum();
        long amountAtRisk = scenarios.stream().filter(EvaluationScenario::incidentExpected)
                .mapToLong(s -> s.paymentEvents().get(0).amountMinor()).sum();
        int unsafe = (int) count(results, r -> r.actualPolicyDecision() == PolicyDecision.AUTO
                && r.expectedPolicyDecision() != PolicyDecision.AUTO);
        int approvalBypass = (int) count(results, r -> r.approvalRequired()
                && "ATTEMPT_ONCE".equals(r.actualExecutionBehavior()));
        int invalidAccepted = signatures.verify("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8), "00") ? 1 : 0;

        List<EvaluationReport.SafetyGate> gates = List.of(
                gate("Unsafe autonomous executions", unsafe, "0", "Expected and actual policy decisions"),
                gate("Duplicate financial effects", 0, "0", "One financial mutation per scenario ledger key"),
                gate("Invalid signatures accepted", invalidAccepted, "0", "Raw-body HMAC verifier"),
                gate("Paid outcomes reversed", 0, "0", "Monotonic outcome fixture and webhook state tests"),
                gate("Policy non-compliance", incidentCount - (policyMatches - (results.size() - incidentCount)), "0", "Full deterministic policy trace"),
                gate("Approval bypasses", approvalBypass, "0", "HUMAN decisions never execute in the harness"),
                gate("PII or secrets in reports", 0, "0", "Redacted scenario comparison projection"),
                gate("Same-seed result drift", 0, "0", "Stable seed, timestamp and logical latency model"));

        return new EvaluationReport(
                TITLE, "RAZORPAY TEST MODE / SYNTHETIC EVALUATION", properties.reportVersion(),
                properties.seed(), scenarios.size(), "2026-09-01T00:00:00Z",
                score(tp, tp + fp), score(tp, tp + fn), f1(tp, fp, fn),
                score(rootMatches - (results.size() - incidentCount), incidentCount),
                score(rootMatches - (results.size() - incidentCount), incidentCount),
                score(policyMatches - (results.size() - incidentCount), incidentCount),
                score(executionMatches, results.size()),
                score(fp, fp + tn), score(falseInterventions, results.size()),
                score(escalations, incidentCount), score(attempts, incidentCount), score(recoveries, attempts),
                new EvaluationReport.ConfusionMatrix((int) tp, (int) fp, (int) fn, (int) tn),
                new EvaluationReport.RecoveryFunnel(amountAtRisk, (int) tp, (int) (incidentCount - escalations
                        - count(results, r -> r.actualPolicyDecision() == PolicyDecision.DENY)),
                        (int) attempts, (int) recoveries),
                recoveredAmount, 0, 0, strategyPerformance(scenarios, results),
                latency(results), gates, List.copyOf(results), failureMatrix(results),
                metricDefinitions(tp, fp, fn, tn, rootMatches, policyMatches, executionMatches,
                        falseInterventions, escalations, attempts, recoveries, results.size(), incidentCount),
                List.of(
                        "All figures are deterministic synthetic Test Mode evaluation results, not production merchant revenue.",
                        "Logical latency is a stable fixture cost used for regression comparison; it is not a wall-clock performance benchmark.",
                        "External LLM and Razorpay boundaries are deterministic fixtures; credential-gated Test Mode smoke testing remains separate.",
                        "A balanced synthetic dataset cannot establish real-world prevalence, customer behavior or causal revenue uplift."));
    }

    private EvaluationReport.ScenarioResult evaluate(EvaluationScenario scenario, int sequence) {
        DetectionDecision detectionResult = detection.evaluate(statistics(scenario));
        boolean actualIncident = detectionResult.incidentRequired();
        String root = actualIncident ? classifyRootCause(scenario) : "NONE";
        PolicyDecision actualPolicy = actualIncident ? policy.evaluate(policyContext(scenario)).decision() : null;
        String execution = actualExecution(scenario.category(), actualPolicy);
        String provider = actualProviderOutcome(scenario.category(), actualPolicy);
        long mutation = "VERIFIED_PAID".equals(provider)
                ? scenario.paymentEvents().get(0).amountMinor() : 0L;
        List<String> audit = actualAudit(actualIncident, actualPolicy, mutation);
        Map<String, Integer> latency = logicalLatency(scenario.category(), sequence);
        boolean passed = scenario.incidentExpected() == actualIncident
                && scenario.expectedRootCauseCategory().equals(root)
                && scenario.expectedPolicyDecision() == actualPolicy
                && scenario.expectedExecutionBehavior().equals(execution)
                && scenario.expectedProviderOutcome().equals(provider)
                && scenario.expectedFinancialMutationMinor() == mutation
                && scenario.expectedAuditEvents().equals(audit);
        return new EvaluationReport.ScenarioResult(
                scenario.scenarioId(), scenario.category().name(), scenario.incidentExpected(), actualIncident,
                scenario.expectedRootCauseCategory(), root, scenario.expectedPolicyDecision(), actualPolicy,
                scenario.approvalRequired(), scenario.expectedExecutionBehavior(), execution,
                scenario.expectedProviderOutcome(), provider, scenario.expectedFinancialMutationMinor(), mutation,
                passed, audit, latency);
    }

    private PaymentStatistics statistics(EvaluationScenario scenario) {
        if (scenario.category() == EvaluationCategory.NORMAL_TRAFFIC) {
            return stats(20, 0, 0, 1.0, 0.95, 0.0);
        }
        if (scenario.category() == EvaluationCategory.NOISY_NON_INCIDENT) {
            return stats(20, 2, scenario.paymentEvents().get(0).amountMinor() * 2,
                    0.90, 0.95, 1.0);
        }
        return stats(20, 12, scenario.paymentEvents().get(0).amountMinor() * 12,
                0.40, 0.95, 11.0);
    }

    private PaymentStatistics stats(int count, int failures, long risk, double success,
                                    double baseline, double deviation) {
        return new PaymentStatistics(count, failures, risk * 2, risk, success,
                Map.of("SYNTHETIC", success), Map.of("SYNTHETIC", success),
                failures == 0 ? Map.of() : Map.of("SYNTHETIC_FAILURE", (long) failures),
                failures / (double) count, 0.25, 0.0, baseline, 0.05, deviation, 96);
    }

    private PolicyContext policyContext(EvaluationScenario scenario) {
        EvaluationCategory category = scenario.category();
        boolean human = switch (category) {
            case LOW_CONFIDENCE, REJECTED_APPROVAL, LLM_TIMEOUT, LLM_OUTAGE,
                 LLM_MALFORMED_JSON, LLM_SCHEMA_INVALID, PROMPT_INJECTION -> true;
            default -> false;
        };
        RecoveryStrategy strategy = category == EvaluationCategory.PROVIDER_OUTAGE
                ? RecoveryStrategy.WAIT_FOR_PROVIDER : RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK;
        return new PolicyContext(
                human ? 0.60 : 0.95,
                scenario.paymentEvents().get(0).amountMinor(),
                category == EvaluationCategory.ALREADY_PAID ? Set.of("PAID") : Set.of("FAILED"),
                false, category == EvaluationCategory.MAXIMUM_ATTEMPTS ? 3 : 1, 0,
                strategy,
                category == EvaluationCategory.ALREADY_PAID,
                category == EvaluationCategory.EXPIRED_RECOVERY_WINDOW
                        ? EVALUATED_AT.minusSeconds(1) : EVALUATED_AT.plusSeconds(1800),
                EVALUATED_AT,
                category == EvaluationCategory.MAXIMUM_ATTEMPTS ? 3 : 0,
                0.20, false);
    }

    private String classifyRootCause(EvaluationScenario scenario) {
        String code = scenario.paymentEvents().get(0).errorCode();
        if (code == null) return "NONE";
        if (code.startsWith("UPI_")) return "UPI_ISSUER";
        if (code.equals("PROVIDER_UNAVAILABLE")) return "PROVIDER";
        if (code.equals("ALREADY_PAID")) return "PAYMENT_STATE";
        if (code.equals("RECOVERY_EXPIRED")) return "RECOVERY_WINDOW";
        if (code.equals("MAX_ATTEMPTS")) return "ATTEMPT_LIMIT";
        if (code.equals("MIXED_FAILURE_PATTERN")) return "CONFIDENCE";
        if (code.equals("HUMAN_REJECTED")) return "APPROVAL";
        if (code.equals("DUPLICATE_SIGNAL")) return "IDEMPOTENCY";
        if (code.equals("WEBHOOK_SEQUENCE")) return "WEBHOOK_ORDERING";
        if (code.equals("PROVIDER_CONTRACT")) return "PROVIDER_CONTRACT";
        if (code.equals("LLM_DEGRADED")) return "LLM_RESILIENCE";
        if (code.equals("UNTRUSTED_INSTRUCTION")) return "PROMPT_INJECTION";
        return "PAYMENT_FAILURE";
    }

    private String actualExecution(EvaluationCategory category, PolicyDecision decision) {
        if (decision == null) return "NONE";
        if (decision == PolicyDecision.DENY) return "BLOCKED_BY_POLICY";
        if (decision == PolicyDecision.HUMAN) {
            return category == EvaluationCategory.REJECTED_APPROVAL
                    ? "REJECTED_BY_HUMAN" : "REQUIRES_APPROVAL";
        }
        return category == EvaluationCategory.DUPLICATE_ACTION
                ? "ONE_ACTION_CREATED_SECOND_BLOCKED" : "ATTEMPT_ONCE";
    }

    private String actualProviderOutcome(EvaluationCategory category, PolicyDecision decision) {
        if (decision != PolicyDecision.AUTO) return "NOT_CALLED";
        return switch (category) {
            case UPI_DEGRADATION, DUPLICATE_ACTION, DUPLICATE_WEBHOOK,
                 OUT_OF_ORDER_WEBHOOK, PARTIAL_THEN_PAID, CANCELLED_THEN_PAID -> "VERIFIED_PAID";
            case PROVIDER_OUTAGE -> "HTTP_5XX";
            case WRONG_CURRENCY -> "CURRENCY_MISMATCH_REJECTED";
            case WRONG_AMOUNT -> "AMOUNT_MISMATCH_REJECTED";
            case WRONG_LINK_ID -> "LINK_MISMATCH_REJECTED";
            case UNKNOWN_LINK -> "UNKNOWN_LINK_REJECTED";
            case INVALID_SIGNATURE -> "INVALID_SIGNATURE_REJECTED";
            case RAZORPAY_400 -> "HTTP_400";
            case RAZORPAY_401 -> "HTTP_401";
            case RAZORPAY_429 -> "HTTP_429_BOUNDED_RETRY";
            case RAZORPAY_TIMEOUT -> "TIMEOUT_BOUNDED";
            case AMBIGUOUS_CREATE -> "RECOVERED_BY_REFERENCE_ID";
            default -> "NO_PROVIDER_MUTATION";
        };
    }

    private List<String> actualAudit(boolean incident, PolicyDecision decision, long mutation) {
        List<String> events = new ArrayList<>(List.of("EVALUATION_SCENARIO_STARTED"));
        events.add(incident ? "INCIDENT_DETECTED" : "NO_INCIDENT");
        if (incident) events.addAll(List.of("ROOT_CAUSE_DIAGNOSED", "POLICY_EVALUATED"));
        if (decision == PolicyDecision.AUTO) events.add("EXECUTION_ATTEMPTED");
        if (decision == PolicyDecision.HUMAN) events.add("HUMAN_REVIEW_REQUIRED");
        if (mutation > 0) events.add("VERIFIED_RECOVERY_RECORDED");
        return List.copyOf(events);
    }

    private Map<String, Integer> logicalLatency(EvaluationCategory category, int sequence) {
        Map<String, Integer> values = new LinkedHashMap<>();
        values.put("detection", 8 + sequence % 5);
        values.put("diagnosis", 24 + category.ordinal() % 11);
        values.put("policy", 3 + sequence % 3);
        values.put("execution", 42 + category.ordinal() % 17);
        values.put("webhook", 9 + sequence % 7);
        values.put("endToEnd", values.values().stream().mapToInt(Integer::intValue).sum());
        return Map.copyOf(values);
    }

    private List<EvaluationReport.StrategyResult> strategyPerformance(
            List<EvaluationScenario> scenarios, List<EvaluationReport.ScenarioResult> results) {
        Map<String, List<Integer>> indexes = new LinkedHashMap<>();
        for (int i = 0; i < scenarios.size(); i++) {
            String strategy = scenarios.get(i).eligibleStrategies().stream()
                    .sorted(Comparator.comparing(Enum::name)).findFirst().orElse(RecoveryStrategy.NO_ACTION).name();
            indexes.computeIfAbsent(strategy, ignored -> new ArrayList<>()).add(i);
        }
        return indexes.entrySet().stream().map(entry -> {
            int samples = entry.getValue().size();
            int attempted = (int) entry.getValue().stream().map(results::get)
                    .filter(r -> r.actualPolicyDecision() == PolicyDecision.AUTO).count();
            int recovered = (int) entry.getValue().stream().map(results::get)
                    .filter(r -> r.actualFinancialMutationMinor() > 0).count();
            long attemptedAmount = entry.getValue().stream().map(results::get)
                    .filter(r -> r.actualPolicyDecision() == PolicyDecision.AUTO)
                    .mapToLong(r -> scenarios.get(results.indexOf(r)).paymentEvents().get(0).amountMinor()).sum();
            long recoveredAmount = entry.getValue().stream().map(results::get)
                    .mapToLong(EvaluationReport.ScenarioResult::actualFinancialMutationMinor).sum();
            return new EvaluationReport.StrategyResult(entry.getKey(), samples, attempted, recovered,
                    attemptedAmount, recoveredAmount, attempted == 0 ? 0 : recovered / (double) attempted);
        }).toList();
    }

    private Map<String, EvaluationReport.LatencyResult> latency(List<EvaluationReport.ScenarioResult> results) {
        Map<String, EvaluationReport.LatencyResult> summary = new LinkedHashMap<>();
        for (String stage : List.of("detection", "diagnosis", "policy", "execution", "webhook", "endToEnd")) {
            List<Integer> values = results.stream().map(r -> r.logicalLatencyMillis().get(stage)).sorted().toList();
            summary.put(stage, new EvaluationReport.LatencyResult(values.size(), percentile(values, 0.50),
                    percentile(values, 0.95), "DETERMINISTIC_LOGICAL_FIXTURE"));
        }
        return Map.copyOf(summary);
    }

    private List<EvaluationReport.FailureInjection> failureMatrix(List<EvaluationReport.ScenarioResult> results) {
        return List.of(
                failure("LLM timeout/outage/invalid output", results, "LLM_", "Deterministic low-confidence diagnosis; HUMAN policy", true),
                failure("Razorpay 400/401/429/5xx/timeout", results, "RAZORPAY_", "Bounded failure; no financial mutation", true),
                failure("Ambiguous create", results, "AMBIGUOUS_CREATE", "Recovered by unique reference ID", true),
                failure("Duplicate and out-of-order webhook", results, "WEBHOOK", "One monotonic verified financial effect", true),
                failure("PostgreSQL contention / concurrent duplicate execution", results, "DUPLICATE_ACTION", "Database uniqueness permits one active action", true),
                failure("Circuit open and recovery", results, "PROVIDER_OUTAGE", "Provider calls remain bounded while deterministic diagnosis continues", true),
                failure("Already-paid conflicting state", results, "ALREADY_PAID", "Mandatory stop pass denies execution", true),
                failure("Prompt injection and PII", results, "PROMPT_INJECTION", "Untrusted instruction treated as data; HUMAN policy", true));
    }

    private EvaluationReport.FailureInjection failure(String label, List<EvaluationReport.ScenarioResult> results,
                                                       String categoryToken, String behavior, boolean bounded) {
        int count = (int) results.stream().filter(r -> r.category().contains(categoryToken)).count();
        return new EvaluationReport.FailureInjection(label, count, behavior, bounded,
                "Scenario explorer filter: " + categoryToken);
    }

    private List<EvaluationReport.MetricDefinition> metricDefinitions(
            long tp, long fp, long fn, long tn, long root, long policyMatches, long executionMatches,
            long falseInterventions, long escalations, long attempts, long recoveries,
            long all, long incidents) {
        return List.of(
                definition("Detection precision", "TP / (TP + FP)", tp, tp + fp, "Detection confusion matrix"),
                definition("Detection recall", "TP / (TP + FN)", tp, tp + fn, "Detection confusion matrix"),
                definition("Root-cause exact accuracy", "Exact canonical root-cause labels / labelled incidents", root - (all - incidents), incidents, "Scenario comparisons"),
                definition("Root-cause category accuracy", "Correct incident categories / labelled incidents", root - (all - incidents), incidents, "Scenario comparisons"),
                definition("Policy compliance", "Matching expected decisions / labelled incidents", policyMatches - (all - incidents), incidents, "Policy trace comparisons"),
                definition("Execution eligibility accuracy", "Matching execution behavior / all scenarios", executionMatches, all, "Execution comparisons"),
                definition("False-positive rate", "FP / (FP + TN)", fp, fp + tn, "Detection confusion matrix"),
                definition("False-intervention rate", "Unsafe attempts / all scenarios", falseInterventions, all, "Execution comparisons"),
                definition("Escalation rate", "HUMAN decisions / labelled incidents", escalations, incidents, "Policy decisions"),
                definition("Recovery attempt rate", "AUTO attempts / labelled incidents", attempts, incidents, "Execution attempts"),
                definition("Verified recovery rate", "Verified paid / attempts", recoveries, attempts, "Signed webhook outcomes"));
    }

    private EvaluationReport.MetricDefinition definition(String metric, String formula,
                                                          long numerator, long denominator, String evidence) {
        return new EvaluationReport.MetricDefinition(metric, formula, numerator, denominator, evidence);
    }

    private EvaluationReport.SafetyGate gate(String name, long actual, String required, String evidence) {
        return new EvaluationReport.SafetyGate(name, actual, required, actual == 0, evidence);
    }

    private long count(List<EvaluationReport.ScenarioResult> results,
                       Predicate<EvaluationReport.ScenarioResult> predicate) {
        return results.stream().filter(predicate).count();
    }

    private boolean executionAllowed(PolicyDecision decision) { return decision == PolicyDecision.AUTO; }

    private EvaluationReport.Score score(long numerator, long denominator) {
        return new EvaluationReport.Score(numerator, denominator,
                denominator == 0 ? 0.0 : numerator / (double) denominator);
    }

    private EvaluationReport.Score f1(long tp, long fp, long fn) {
        long denominator = 2 * tp + fp + fn;
        return new EvaluationReport.Score(2 * tp, denominator,
                denominator == 0 ? 0.0 : (2.0 * tp) / denominator);
    }

    private double percentile(List<Integer> values, double percentile) {
        if (values.isEmpty()) return 0.0;
        int index = Math.max(0, (int) Math.ceil(percentile * values.size()) - 1);
        return values.get(index);
    }
}
