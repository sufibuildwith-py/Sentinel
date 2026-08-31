package com.sentinel.revenue.governor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicRecoveryGovernorTest {
    private final DynamicRecoveryGovernor governor = new DynamicRecoveryGovernor();

    @Test
    void transitionsFromGreenToCanaryHumanAndHaltUsingMeasuredPressure() {
        assertThat(assess(0.1).posture()).isEqualTo(GovernorPosture.GREEN);
        assertThat(assess(0.85).posture()).isEqualTo(GovernorPosture.YELLOW);
        assertThat(assess(1.0).posture()).isEqualTo(GovernorPosture.ORANGE);
        assertThat(assess(1.6).posture()).isEqualTo(GovernorPosture.RED);
    }

    private DynamicGovernorAssessment assess(double ratio) {
        return governor.assess(new GovernorSignalSnapshot(ratio * 0.25, 0, 0, 100, 100), 0.25);
    }
}
