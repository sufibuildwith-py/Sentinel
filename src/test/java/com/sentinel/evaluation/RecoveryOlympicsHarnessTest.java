package com.sentinel.evaluation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecoveryOlympicsHarnessTest {
    private final RecoveryOlympicsDataset dataset = new RecoveryOlympicsDataset();
    private final RecoveryOlympicsHarness harness = new RecoveryOlympicsHarness(dataset);

    @Test
    void producesFrozenDeterministicTenThousandCaseBenchmark() {
        RecoveryOlympicsReport first = harness.evaluate();
        RecoveryOlympicsReport second = harness.evaluate();

        assertThat(first).isEqualTo(second);
        assertThat(first.datasetSize()).isEqualTo(10_000);
        assertThat(first.seed()).isEqualTo(20_260_901L);
        assertThat(first.frozenSplit()).containsEntry(RecoveryOlympicsSplit.DEVELOPMENT, 7_000)
                .containsEntry(RecoveryOlympicsSplit.HELD_OUT, 2_000)
                .containsEntry(RecoveryOlympicsSplit.ADVERSARIAL, 1_000);
        assertThat(first.arms()).hasSize(7);
    }

    @Test
    void labelsApproximationsAndKeepsSentinelV2Safe() {
        RecoveryOlympicsReport report = harness.evaluate();
        assertThat(report.arms()).filteredOn(result -> result.arm().equals("D") || result.arm().equals("E"))
                .allMatch(result -> result.methodologyLabel().equals("DOCUMENTED_APPROXIMATION"));

        RecoveryOlympicsReport.ArmResult sentinel = report.arms().stream()
                .filter(result -> result.arm().equals("G")).findFirst().orElseThrow();
        assertThat(sentinel.unsafeExecutions()).isZero();
        assertThat(sentinel.policyViolations()).isZero();
        assertThat(sentinel.duplicateFinancialEffects()).isZero();
        assertThat(sentinel.refusals()).isPositive();
        assertThat(sentinel.noActions()).isPositive();
        assertThat(sentinel.incrementalRecoveryRate().method()).isEqualTo("WILSON_95_PERCENT");
    }

    @Test
    void identicalCasesFeedEveryArmAndNoArmHidesNaturalRecovery() {
        RecoveryOlympicsReport report = harness.evaluate();
        long natural = report.arms().get(0).naturalRecoveryMinor();
        assertThat(report.arms()).allMatch(result -> result.sampleCount() == 10_000)
                .allMatch(result -> result.naturalRecoveryMinor() == natural)
                .allMatch(result -> result.grossRecoveryMinor() >= result.naturalRecoveryMinor())
                .allMatch(result -> result.netIncrementalValueMinor()
                        == result.incrementalRecoveryMinor() - result.recoveryCostMinor());
    }
}
