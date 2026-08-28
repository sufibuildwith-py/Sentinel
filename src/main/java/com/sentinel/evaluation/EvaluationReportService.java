package com.sentinel.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class EvaluationReportService {
    private final EvaluationHarness harness;
    private final EvaluationReportFormatter formatter;
    private final EvaluationRunRepository runs;
    private final SensitiveDataScanner sensitiveData;
    private final ObjectMapper canonicalMapper;

    public EvaluationReportService(EvaluationHarness harness, EvaluationReportFormatter formatter,
                                   EvaluationRunRepository runs, SensitiveDataScanner sensitiveData,
                                   ObjectMapper objectMapper) {
        this.harness = harness;
        this.formatter = formatter;
        this.runs = runs;
        this.sensitiveData = sensitiveData;
        this.canonicalMapper = objectMapper.copy()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    public EvaluationReport report() { return harness.evaluate(); }

    public String json(EvaluationReport report) {
        try { return canonicalMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report); }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Evaluation report serialization failed", exception);
        }
    }

    public String markdown(EvaluationReport report) { return formatter.markdown(report); }

    @Transactional
    public EvaluationRun runAndPersist() {
        EvaluationReport report = report();
        String json = json(report);
        String replayJson = json(report());
        if (!json.equals(replayJson)) {
            throw new IllegalStateException("Evaluation report failed same-seed determinism gate");
        }
        validate(report);
        String markdown = markdown(report);
        if (!sensitiveData.findings(json + "\n" + markdown).isEmpty()) {
            throw new IllegalStateException("Evaluation report failed sensitive-data gate");
        }
        String hash = sha256(json + "\n" + markdown);
        return runs.findByReportVersionAndSeedAndReportSha256(report.reportVersion(), report.seed(), hash)
                .orElseGet(() -> runs.saveAndFlush(new EvaluationRun(
                        UUID.randomUUID(), report.reportVersion(), report.seed(), report.datasetSize(),
                        hash, json, markdown, Instant.now())));
    }

    private void validate(EvaluationReport report) {
        if (report.datasetSize() != report.scenarios().size()) {
            throw new IllegalStateException("Evaluation dataset size does not match scenario evidence");
        }
        List<EvaluationReport.Score> scores = List.of(
                report.detectionPrecision(), report.detectionRecall(), report.detectionF1(),
                report.rootCauseExactAccuracy(), report.rootCauseCategoryAccuracy(), report.policyCompliance(),
                report.executionEligibilityAccuracy(), report.falsePositiveRate(),
                report.falseInterventionRate(), report.escalationRate(),
                report.recoveryAttemptRate(), report.verifiedRecoveryRate());
        for (EvaluationReport.Score score : scores) {
            double calculated = score.denominator() == 0 ? 0.0
                    : score.numerator() / (double) score.denominator();
            if (Math.abs(calculated - score.value()) > 0.000000001) {
                throw new IllegalStateException("Evaluation metric numerator/denominator mismatch");
            }
        }
        boolean scenarioFailure = report.scenarios().stream().anyMatch(result ->
                !result.passed() || result.auditEvents().isEmpty());
        boolean gateFailure = report.safetyGates().stream().anyMatch(gate -> !gate.passed());
        if (scenarioFailure || gateFailure) {
            throw new IllegalStateException("Evaluation safety or scenario evidence gate failed");
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
