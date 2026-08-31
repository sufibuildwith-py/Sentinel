package com.sentinel.revenue.failurelab;

import com.sentinel.evaluation.EvaluationReport;
import com.sentinel.evaluation.EvaluationReportService;
import com.sentinel.revenue.model.ShadowDecisionDifference;
import com.sentinel.revenue.repository.ShadowDecisionDifferenceRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.*;

@Service
public class FailureLabService {
    private static final List<FailureLabScenario> CATALOG = List.of(
            scenario("duplicate-webhook", "Duplicate webhook", "Proves at-least-once delivery creates one financial effect.", FailureLabMode.FAULT_INJECTION, "IDEMPOTENT_ACK", "Duplicate and out-of-order webhook"),
            scenario("out-of-order-webhook", "Out-of-order webhook", "Proves monotonic reconciliation rejects state regression.", FailureLabMode.FAULT_INJECTION, "MONOTONIC_STATE", "Duplicate and out-of-order webhook"),
            scenario("invalid-signature", "Invalid signature", "Proves unverified provider input cannot mutate revenue truth.", FailureLabMode.FAULT_INJECTION, "HTTP_400_NO_MUTATION", "WEBHOOK"),
            scenario("provider-timeout", "Provider timeout", "Proves timeout is bounded and does not imply acceptance.", FailureLabMode.FAULT_INJECTION, "EXECUTION_UNCERTAIN", "Razorpay 400/401/429/5xx/timeout"),
            scenario("provider-5xx", "Provider 5xx", "Proves retry remains bounded with no financial mutation.", FailureLabMode.FAULT_INJECTION, "BOUNDED_RETRY", "Razorpay 400/401/429/5xx/timeout"),
            scenario("payment-downtime", "Payment downtime", "Routes degraded instruments away from unsafe immediate recovery.", FailureLabMode.SYNTHETIC_BENCHMARK, "WAIT_OR_ALTERNATE", "PROVIDER_OUTAGE"),
            scenario("systemic-upi-degradation", "Systemic UPI / bank degradation", "Correlates merchant-wide degradation before recovery planning.", FailureLabMode.SYNTHETIC_BENCHMARK, "SYSTEMIC_INCIDENT", "UPI"),
            scenario("policy-deny", "Policy DENY", "Treats refusal as success when deterministic policy blocks an unsafe action.", FailureLabMode.SYNTHETIC_BENCHMARK, "DENY", "ALREADY_PAID"),
            scenario("blast-radius-denial", "Blast-radius denial", "Proves an execution envelope can stop an otherwise eligible action.", FailureLabMode.SYNTHETIC_BENCHMARK, "GOVERNOR_DENY", "DUPLICATE_ACTION"),
            scenario("kill-switch", "Kill switch", "Shows autonomous execution can be stopped independently of model confidence.", FailureLabMode.SYNTHETIC_BENCHMARK, "STOPPED", "PROVIDER_OUTAGE"),
            scenario("canary-held", "Canary held", "Prevents batch expansion until provider-confirmed reconciliation reaches the gate.", FailureLabMode.SYNTHETIC_BENCHMARK, "CANARY_NOT_EXPANDED", "execution"),
            scenario("communication-blocked", "Communication blocked", "Consent, DNC, and quiet-hour denial is a successful safety result.", FailureLabMode.SYNTHETIC_BENCHMARK, "CONTACT_DENIED", "policy"),
            scenario("unsupported-action", "Unsupported recovery action", "Capability registry refuses a tool path the provider cannot support.", FailureLabMode.SYNTHETIC_BENCHMARK, "NO_ACTION", "execution"),
            scenario("ambiguous-acceptance", "Ambiguous provider acceptance", "Recovers by unique reference without claiming revenue recovery.", FailureLabMode.FAULT_INJECTION, "AWAITING_RECONCILIATION", "Ambiguous create"),
            scenario("accepted-not-recovered", "Provider accepted, not recovered", "Separates accepted execution from provider-confirmed money.", FailureLabMode.REAL_RAZORPAY_TEST_MODE, "AWAITING_RECONCILIATION", "provider confirmation", false),
            scenario("challenger-loses", "Challenger loses to champion", "A model or policy challenger remains shadow-only when evidence regresses.", FailureLabMode.SHADOW_ONLY, "PROMOTION_BLOCKED", "shadow", true),
            scenario("evidence-refusal", "Evidence Capsule refusal", "Shows the exact evidence and rule trace behind a safe refusal.", FailureLabMode.SYNTHETIC_BENCHMARK, "REFUSAL_EXPLAINED", "ALREADY_PAID")
    );

    private final EvaluationReportService reports;
    private final ShadowDecisionDifferenceRepository shadowDifferences;
    private final Clock clock;

    public FailureLabService(EvaluationReportService reports,
                             ShadowDecisionDifferenceRepository shadowDifferences, Clock clock) {
        this.reports = reports;
        this.shadowDifferences = shadowDifferences;
        this.clock = clock;
    }

    public List<FailureLabScenario> scenarios() { return CATALOG; }

    public FailureLabResult run(String id) {
        FailureLabScenario scenario = CATALOG.stream().filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Failure Lab scenario: " + id));
        if (!scenario.runnable()) {
            return new FailureLabResult(scenario, "REQUIRES_REAL_PROVIDER_EVENT", false,
                    "No outcome was synthesized. Complete this scenario with a signed Razorpay Test Mode event.",
                    List.of("REAL RAZORPAY TEST MODE", "AWAITING RECONCILIATION"), clock.instant());
        }
        if (scenario.mode() == FailureLabMode.SHADOW_ONLY) return shadowResult(scenario);
        return evaluationResult(scenario, reports.report());
    }

    private FailureLabResult shadowResult(FailureLabScenario scenario) {
        List<ShadowDecisionDifference> differences = shadowDifferences.findAll();
        List<ShadowDecisionDifference> regressions = differences.stream()
                .filter(ShadowDecisionDifference::isCriticalRegression).toList();
        boolean evidenced = !differences.isEmpty();
        String behavior = regressions.isEmpty()
                ? "No critical shadow regression is recorded; promotion still requires persisted approval evidence."
                : regressions.size() + " critical shadow regression(s) block promotion.";
        return new FailureLabResult(scenario, evidenced ? "EVIDENCED" : "NO_RECORDED_SHADOW_EVIDENCE",
                evidenced, behavior, differences.stream().limit(5)
                .map(ShadowDecisionDifference::getExplanation).toList(), clock.instant());
    }

    private FailureLabResult evaluationResult(FailureLabScenario scenario, EvaluationReport report) {
        String selector = scenario.evidenceSelector().toLowerCase(Locale.ROOT);
        List<String> evidence = new ArrayList<>();
        report.failureInjectionMatrix().stream()
                .filter(item -> searchable(item.failure(), selector) || searchable(item.evidence(), selector))
                .forEach(item -> evidence.add(item.failure() + ": " + item.observedBehavior()
                        + " · bounded=" + item.bounded()));
        report.scenarios().stream()
                .filter(item -> searchable(item.scenarioId(), selector)
                        || searchable(item.category(), selector)
                        || searchable(item.actualExecutionBehavior(), selector))
                .limit(8).forEach(item -> evidence.add(item.scenarioId() + ": "
                        + item.actualExecutionBehavior() + " · passed=" + item.passed()));
        if (evidence.isEmpty() && ("execution".equals(selector) || "policy".equals(selector))) {
            evidence.add("Evaluation gates: policyCompliance=" + report.policyCompliance().numerator()
                    + "/" + report.policyCompliance().denominator()
                    + ", duplicateFinancialEffects=" + report.duplicateFinancialEffects());
        }
        boolean passed = !evidence.isEmpty() && report.safetyGates().stream().allMatch(EvaluationReport.SafetyGate::passed);
        return new FailureLabResult(scenario, passed ? "EVIDENCED" : "EVIDENCE_NOT_FOUND", passed,
                passed ? "Expected refusal or bounded behavior is backed by the existing deterministic evaluation harness."
                        : "The current evaluation report does not contain matching evidence; no safe outcome is claimed.",
                evidence, clock.instant());
    }

    private boolean searchable(String value, String selector) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(selector);
    }

    private static FailureLabScenario scenario(String id, String title, String description,
                                               FailureLabMode mode, String outcome, String selector) {
        return scenario(id, title, description, mode, outcome, selector, true);
    }

    private static FailureLabScenario scenario(String id, String title, String description,
                                               FailureLabMode mode, String outcome,
                                               String selector, boolean runnable) {
        return new FailureLabScenario(id, title, description, mode, outcome, selector, runnable);
    }
}
