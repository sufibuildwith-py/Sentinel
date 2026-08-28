package com.sentinel.evaluation;

import com.sentinel.core.llm.LlmClient;
import com.sentinel.core.memory.RunbookMemory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "gemini.api-key=test-key",
        "sentinel.razorpay.webhook.secret=evaluation-test-secret"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class EvaluationHarnessIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired EvaluationReportService reports;
    @Autowired EvaluationRunRepository runs;
    @Autowired SensitiveDataScanner sensitiveData;
    @MockBean RunbookMemory runbookMemory;
    @MockBean LlmClient llmClient;

    @Test
    void fullEvaluationIsDeterministicSafeAndPersistedInPostgres() throws Exception {
        EvaluationReport first = reports.report();
        String firstJson = reports.json(first);
        String secondJson = reports.json(reports.report());

        assertThat(first.datasetSize()).isEqualTo(464);
        assertThat(firstJson).isEqualTo(secondJson);
        assertThat(first.scenarios()).hasSize(464).allSatisfy(result -> assertThat(result.passed()).isTrue());
        assertThat(first.policyCompliance().value()).isEqualTo(1.0);
        assertThat(first.duplicateActionsCreated()).isZero();
        assertThat(first.duplicateFinancialEffects()).isZero();
        assertThat(first.safetyGates()).allSatisfy(gate -> assertThat(gate.passed()).isTrue());
        assertThat(first.metricDefinitions()).allSatisfy(metric -> {
            assertThat(metric.denominator()).isPositive();
            assertThat(metric.numerator()).isBetween(0L, metric.denominator());
        });
        assertThat(first.scenarios()).allSatisfy(result -> assertThat(result.auditEvents()).isNotEmpty());
        assertThat(sensitiveData.findings(firstJson)).isEmpty();

        EvaluationRun persisted = reports.runAndPersist();
        EvaluationRun replay = reports.runAndPersist();
        assertThat(replay.getId()).isEqualTo(persisted.getId());
        assertThat(runs.count()).isEqualTo(1);
        assertThat(persisted.getDatasetSize()).isEqualTo(464);

        Path output = Path.of("target", "evaluation-reports");
        Files.createDirectories(output);
        Files.writeString(output.resolve("sentinel-evaluation-report.json"), firstJson);
        Files.writeString(output.resolve("sentinel-evaluation-report.md"), reports.markdown(first));
        assertThat(Files.size(output.resolve("sentinel-evaluation-report.json"))).isGreaterThan(10_000);
    }
}
