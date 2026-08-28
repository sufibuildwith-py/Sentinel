package com.sentinel.evaluation;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class EvaluationReportFormatter {
    public String markdown(EvaluationReport report) {
        StringBuilder out = new StringBuilder();
        out.append("# ").append(report.title()).append("\n\n")
                .append("Report version: `").append(report.reportVersion()).append("`  \n")
                .append("Seed: `").append(report.seed()).append("`  \n")
                .append("Dataset: **").append(report.datasetSize()).append(" labelled scenarios**  \n")
                .append("Scope: **").append(report.scopeLabel()).append("**\n\n")
                .append("## Executive scorecard\n\n")
                .append("| Metric | Result | Numerator / denominator |\n")
                .append("| --- | ---: | ---: |\n");
        metric(out, "Detection precision", report.detectionPrecision());
        metric(out, "Detection recall", report.detectionRecall());
        metric(out, "Detection F1", report.detectionF1());
        metric(out, "Root-cause exact accuracy", report.rootCauseExactAccuracy());
        metric(out, "Root-cause category accuracy", report.rootCauseCategoryAccuracy());
        metric(out, "Policy compliance", report.policyCompliance());
        metric(out, "Execution eligibility accuracy", report.executionEligibilityAccuracy());
        metric(out, "False-intervention rate", report.falseInterventionRate());
        metric(out, "Verified recovery rate", report.verifiedRecoveryRate());

        out.append("\n## Safety gates\n\n| Gate | Actual | Required | Result | Evidence |\n")
                .append("| --- | ---: | ---: | --- | --- |\n");
        report.safetyGates().forEach(gate -> out.append("| ").append(gate.gate()).append(" | ")
                .append(gate.actual()).append(" | ").append(gate.required()).append(" | ")
                .append(gate.passed() ? "PASS" : "FAIL").append(" | ")
                .append(gate.evidence()).append(" |\n"));

        out.append("\n## Recovery evidence\n\n")
                .append("- Amount at risk: ").append(report.recoveryFunnel().amountAtRiskMinor()).append(" minor units\n")
                .append("- Attempted scenarios: ").append(report.recoveryFunnel().attempted()).append("\n")
                .append("- Verified recovered scenarios: ").append(report.recoveryFunnel().verifiedRecovered()).append("\n")
                .append("- Verified recovered amount: ").append(report.recoveredAmountMinor()).append(" minor units\n")
                .append("- Duplicate financial effects: ").append(report.duplicateFinancialEffects()).append("\n\n")
                .append("## Strategy comparison\n\n| Strategy | Samples | Attempted | Recovered | Rate |\n")
                .append("| --- | ---: | ---: | ---: | ---: |\n");
        report.strategyPerformance().forEach(strategy -> out.append("| ").append(strategy.strategy())
                .append(" | ").append(strategy.sampleCount()).append(" | ").append(strategy.attemptedCount())
                .append(" | ").append(strategy.recoveredCount()).append(" | ")
                .append(percent(strategy.recoveryRate())).append(" |\n"));

        out.append("\n## Latency\n\nLogical fixture latency is deterministic regression evidence, not a production benchmark.\n\n")
                .append("| Stage | Samples | p50 ms | p95 ms | Mode |\n")
                .append("| --- | ---: | ---: | ---: | --- |\n");
        report.latencyMillis().forEach((stage, latency) -> out.append("| ").append(stage)
                .append(" | ").append(latency.sampleCount()).append(" | ").append(latency.p50())
                .append(" | ").append(latency.p95()).append(" | ").append(latency.measurementMode()).append(" |\n"));

        out.append("\n## What this does not prove\n\n");
        report.limitations().forEach(limitation -> out.append("- ").append(limitation).append("\n"));
        return out.toString();
    }

    private void metric(StringBuilder out, String label, EvaluationReport.Score score) {
        out.append("| ").append(label).append(" | ").append(percent(score.value())).append(" | ")
                .append(score.numerator()).append(" / ").append(score.denominator()).append(" |\n");
    }

    private String percent(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * 100.0);
    }
}
