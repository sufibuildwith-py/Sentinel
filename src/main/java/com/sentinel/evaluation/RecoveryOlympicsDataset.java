package com.sentinel.evaluation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

@Component
public class RecoveryOlympicsDataset {
    public static final long SEED = 20_260_901L;
    public static final int SIZE = 10_000;
    public static final String VERSION = "recovery-olympics-v1";

    public List<RecoveryOlympicsCase> generate() {
        SplittableRandom random = new SplittableRandom(SEED);
        List<RecoveryOlympicsCase> cases = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            RecoveryOlympicsSplit split = i < 7_000 ? RecoveryOlympicsSplit.DEVELOPMENT
                    : i < 9_000 ? RecoveryOlympicsSplit.HELD_OUT : RecoveryOlympicsSplit.ADVERSARIAL;
            boolean adversarial = split == RecoveryOlympicsSplit.ADVERSARIAL;
            long amount = 5_000L + random.nextLong(495_001L);
            boolean alreadyPaid = random.nextDouble() < (adversarial ? 0.18 : 0.05);
            boolean duplicateRisk = random.nextDouble() < (adversarial ? 0.22 : 0.04);
            boolean unacceptableRisk = random.nextDouble() < (adversarial ? 0.16 : 0.03);
            boolean contactAllowed = random.nextDouble() >= (adversarial ? 0.20 : 0.08);
            boolean providerHealthy = random.nextDouble() >= (adversarial ? 0.15 : 0.05);
            double naturalProbability = 0.18 + random.nextDouble() * 0.24;
            boolean natural = alreadyPaid || random.nextDouble() < naturalProbability;
            double predicted = 0.20 + random.nextDouble() * 0.68;
            double lift = providerHealthy && contactAllowed ? 0.08 + predicted * 0.30 : 0.0;
            boolean treatment = natural || random.nextDouble() < lift;
            long cost = 35L + random.nextLong(166L);
            cases.add(new RecoveryOlympicsCase(i, split, amount, natural, treatment, alreadyPaid,
                    duplicateRisk, unacceptableRisk, contactAllowed, providerHealthy, predicted, cost,
                    20L + random.nextLong(1_421L), 5L + random.nextLong(236L)));
        }
        return List.copyOf(cases);
    }
}
