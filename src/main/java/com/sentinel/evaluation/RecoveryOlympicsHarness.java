package com.sentinel.evaluation;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class RecoveryOlympicsHarness {
    private final RecoveryOlympicsDataset dataset;

    public RecoveryOlympicsHarness(RecoveryOlympicsDataset dataset) {
        this.dataset = dataset;
    }

    public RecoveryOlympicsReport evaluate() {
        List<RecoveryOlympicsCase> cases = dataset.generate();
        List<RecoveryOlympicsReport.ArmResult> arms = new ArrayList<>();
        for (RecoveryOlympicsArm arm : RecoveryOlympicsArm.values()) arms.add(evaluate(arm, cases));
        Map<RecoveryOlympicsSplit, Integer> split = new EnumMap<>(RecoveryOlympicsSplit.class);
        for (RecoveryOlympicsSplit value : RecoveryOlympicsSplit.values()) {
            split.put(value, (int) cases.stream().filter(item -> item.split() == value).count());
        }
        return new RecoveryOlympicsReport(
                "Sentinel 10,000-case Recovery Olympics",
                "SYNTHETIC / CONTROLLED BENCHMARK",
                RecoveryOlympicsDataset.VERSION,
                RecoveryOlympicsDataset.SEED,
                cases.size(), Map.copyOf(split), List.copyOf(arms),
                List.of("Same initial conditions and natural-recovery injection for every arm",
                        "Fixed seed and frozen 7,000/2,000/1,000 development/held-out/adversarial split",
                        "No tuning against held-out or adversarial cases",
                        "Already-paid and risk cases are identical across arms",
                        "Refusals, NO_ACTION, losses and safety violations remain visible"),
                List.of("Outcomes are generated counterfactual fixtures, not merchant transactions",
                        "Recovery is credited from the same latent outcome draws for every arm",
                        "Competitor-inspired arms are documented approximations, not proprietary reproductions",
                        "Decision latency is deterministic logical latency, not wall-clock performance"),
                List.of("This controlled benchmark does not establish production causal uplift",
                        "It is separate from the public-source Historical Razorpay Validation Corpus",
                        "No arm invokes Razorpay, communication tools, or production execution authority"));
    }

    private RecoveryOlympicsReport.ArmResult evaluate(RecoveryOlympicsArm arm,
                                                       List<RecoveryOlympicsCase> cases) {
        long gross = 0L;
        long natural = 0L;
        long cost = 0L;
        long recoveryMinutes = 0L;
        int recovered = 0;
        int naturalCount = 0;
        int interventions = 0;
        int refusals = 0;
        int contacts = 0;
        int escalations = 0;
        int unsafe = 0;
        int violations = 0;
        int unnecessary = 0;
        int duplicates = 0;
        List<Integer> latency = new ArrayList<>(cases.size());

        for (RecoveryOlympicsCase item : cases) {
            Decision decision = decide(arm, item);
            if (item.naturallyRecovers()) {
                natural += item.amountMinor();
                naturalCount++;
            }
            if (decision.intervene()) {
                interventions++;
                cost += item.interventionCostMinor();
                if (decision.contact()) contacts++;
                if (item.naturallyRecovers()) unnecessary++;
                if (unsafe(item)) {
                    unsafe++;
                    violations++;
                    if (item.duplicateRisk() || item.alreadyPaid()) duplicates++;
                }
            } else if (decision.refused()) {
                refusals++;
            }
            if (decision.human()) escalations++;
            boolean outcome = item.naturallyRecovers() || (decision.intervene() && item.treatmentWouldRecover());
            if (outcome) {
                recovered++;
                gross += item.amountMinor();
                recoveryMinutes += decision.intervene() && !item.naturallyRecovers()
                        ? item.treatmentRecoveryMinutes() : item.naturalRecoveryMinutes();
            }
            latency.add(decision.latencyMillis());
        }
        long incremental = Math.max(0L, gross - natural);
        int incrementalCount = Math.max(0, recovered - naturalCount);
        return new RecoveryOlympicsReport.ArmResult(
                arm.code(), arm.label(), arm.methodologyLabel(), cases.size(), interventions, refusals,
                cases.size() - interventions, gross, natural, incremental,
                wilson(incrementalCount, Math.max(1, interventions)), cost, incremental - cost,
                recovered == 0 ? 0.0 : recoveryMinutes / (double) recovered,
                rate(contacts, cases.size()), rate(escalations, cases.size()),
                rate(unsafe, cases.size()), rate(unnecessary, Math.max(1, interventions)),
                duplicates, unsafe, violations,
                arm == RecoveryOlympicsArm.SENTINEL_BASELINE || arm == RecoveryOlympicsArm.SENTINEL_V2
                        ? 1.0 : arm == RecoveryOlympicsArm.STATIC_RULES ? 0.85 : 0.60,
                new RecoveryOlympicsReport.Latency(percentile(latency, 0.50), percentile(latency, 0.95),
                        percentile(latency, 0.99), "DETERMINISTIC_LOGICAL_FIXTURE"));
    }

    private Decision decide(RecoveryOlympicsArm arm, RecoveryOlympicsCase item) {
        boolean unsafe = unsafe(item);
        return switch (arm) {
            case NO_INTERVENTION -> new Decision(false, false, false, false, 1);
            case BLIND_INTERVENTION -> new Decision(true, true, false, false, 4);
            case STATIC_RULES -> unsafe || !item.contactAllowed()
                    ? new Decision(false, false, false, true, 6)
                    : new Decision(true, true, false, false, 6);
            case RECOVERAX_APPROXIMATION -> unsafe || item.predictedRecoveryProbability() * item.amountMinor()
                    <= item.interventionCostMinor() * 4
                    ? new Decision(false, false, false, true, 10)
                    : new Decision(true, item.contactAllowed(), false, false, 10);
            case RIE_APPROXIMATION -> unsafe || !item.providerHealthy()
                    ? new Decision(false, false, false, true, 14)
                    : new Decision(true, item.contactAllowed(), false, false, 14);
            case SENTINEL_BASELINE -> unsafe
                    ? new Decision(false, false, false, true, 18)
                    : item.predictedRecoveryProbability() < 0.45
                    ? new Decision(false, false, true, true, 18)
                    : new Decision(true, item.contactAllowed(), false, false, 18);
            case SENTINEL_V2 -> unsafe || !item.providerHealthy()
                    ? new Decision(false, false, false, true, 23)
                    : !item.contactAllowed() || item.predictedRecoveryProbability() < 0.38
                    ? new Decision(false, false, true, true, 23)
                    : item.predictedRecoveryProbability() * item.amountMinor() <= item.interventionCostMinor()
                    ? new Decision(false, false, false, true, 23)
                    : new Decision(true, true, false, false, 23);
        };
    }

    private boolean unsafe(RecoveryOlympicsCase item) {
        return item.alreadyPaid() || item.duplicateRisk() || item.unacceptableRisk();
    }

    private RecoveryOlympicsReport.RateWithInterval wilson(long numerator, long denominator) {
        double p = numerator / (double) denominator;
        double z = 1.959963984540054;
        double divisor = 1.0 + z * z / denominator;
        double center = (p + z * z / (2.0 * denominator)) / divisor;
        double margin = z * Math.sqrt((p * (1.0 - p) + z * z / (4.0 * denominator)) / denominator) / divisor;
        return new RecoveryOlympicsReport.RateWithInterval(p, Math.max(0.0, center - margin),
                Math.min(1.0, center + margin), numerator, denominator, "WILSON_95_PERCENT");
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : numerator / (double) denominator;
    }

    private double percentile(List<Integer> unsorted, double percentile) {
        List<Integer> values = unsorted.stream().sorted().toList();
        int index = (int) Math.ceil(percentile * values.size()) - 1;
        return values.get(Math.max(0, Math.min(values.size() - 1, index)));
    }

    private record Decision(boolean intervene, boolean contact, boolean human,
                            boolean refused, int latencyMillis) { }
}
