package com.sentinel.evaluation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationSafetyMatrixTest {
    private final EvaluationDatasetGenerator dataset = new EvaluationDatasetGenerator(
            new EvaluationProperties(20_260_901L, 16, "phase9-v1"));

    @ParameterizedTest
    @EnumSource(EvaluationCategory.class)
    void everyRequiredCategoryHasSixteenIndependentOracleScenarios(EvaluationCategory category) {
        assertThat(dataset.generate()).filteredOn(scenario -> scenario.category() == category)
                .hasSize(16)
                .allSatisfy(scenario -> {
                    assertThat(scenario.scenarioId()).startsWith("eval_");
                    assertThat(scenario.paymentEvents()).isNotEmpty();
                    assertThat(scenario.expectedAuditEvents()).isNotEmpty();
                    assertThat(scenario.expectedProviderOutcome()).isNotBlank();
                    assertThat(scenario.expectedExecutionBehavior()).isNotBlank();
                });
    }
}
