package com.sentinel.revenue.experiment;

import com.sentinel.revenue.economics.EconomicEvidenceQuality;
import com.sentinel.revenue.opportunity.OpportunityAction;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledExperimentEngineTest {
    private final ControlledExperimentEngine engine = new ControlledExperimentEngine();

    @Test
    void assignmentIsDeterministicAndSafetyIneligibleCaseGetsNoAuthority() {
        ExperimentDefinition definition = definition(1);
        ExperimentCandidate safe = new ExperimentCandidate(UUID.randomUUID(), 1_000, true, true, false);
        ExperimentCandidate blocked = new ExperimentCandidate(UUID.randomUUID(), 1_000, false, true, false);
        List<ExperimentAssignment> first = engine.assign(definition, List.of(safe, blocked));
        List<ExperimentAssignment> second = engine.assign(definition, List.of(safe, blocked));

        assertThat(first).isEqualTo(second);
        assertThat(first).filteredOn(item -> item.incidentId().equals(blocked.incidentId()))
                .singleElement().satisfies(item -> {
                    assertThat(item.action()).isEqualTo(OpportunityAction.NO_ACTION);
                    assertThat(item.authorityState()).isEqualTo("NO_AUTHORITY");
                });
    }

    @Test
    void controlledHoldoutLabelRequiresMinimumSamplesAndProviderConfirmation() {
        ExperimentDefinition definition = definition(1);
        List<ExperimentObservation> observations = List.of(
                new ExperimentObservation(UUID.randomUUID(), "CONTROL", true, true, 1000, 0,
                        Duration.ofMinutes(5), false, false),
                new ExperimentObservation(UUID.randomUUID(), "TREATMENT", false, true, 1500, 100,
                        Duration.ofMinutes(3), true, false));
        ExperimentSummary summary = engine.summarize(definition, observations);
        assertThat(summary.evidenceQuality()).isEqualTo(EconomicEvidenceQuality.CONTROLLED_HOLDOUT);
        assertThat(summary.authorityState()).isEqualTo("NO_LIVE_AUTHORITY_CHANGE");
        assertThat(summary.arms()).allSatisfy(arm -> assertThat(arm.samples()).isEqualTo(1));
    }

    private ExperimentDefinition definition(int minimum) {
        return new ExperimentDefinition(UUID.randomUUID(), "holdout", 20260901L, List.of(
                new ExperimentArm("CONTROL", OpportunityAction.NO_ACTION, 70, true, "control-v1"),
                new ExperimentArm("TREATMENT", OpportunityAction.CREATE_PAYMENT_LINK, 30, false, "link-v1")),
                100_000, minimum, 0.05, "merchant-approval-1", "policy-v1", "model-v1");
    }
}
