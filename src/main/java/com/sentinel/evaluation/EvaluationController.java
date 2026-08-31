package com.sentinel.evaluation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/evaluation")
public class EvaluationController {
    private final EvaluationReportService reports;
    private final RecoveryOlympicsHarness recoveryOlympics;
    private final HistoricalValidationRunner historicalValidation;

    public EvaluationController(EvaluationReportService reports, RecoveryOlympicsHarness recoveryOlympics,
                                HistoricalValidationRunner historicalValidation) {
        this.reports = reports;
        this.recoveryOlympics = recoveryOlympics;
        this.historicalValidation = historicalValidation;
    }

    @GetMapping("/report")
    public EvaluationReport report() { return reports.report(); }

    @PostMapping("/run")
    public EvaluationReport run() {
        reports.runAndPersist();
        return reports.report();
    }

    @GetMapping("/recovery-olympics")
    public RecoveryOlympicsReport recoveryOlympics() { return recoveryOlympics.evaluate(); }

    @GetMapping("/historical")
    public HistoricalValidationReport historicalValidation() { return historicalValidation.evaluate(); }

    @GetMapping(value = "/report.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> jsonDownload() {
        byte[] body = reports.json(reports.report()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sentinel-evaluation-report.json")
                .contentType(MediaType.APPLICATION_JSON).body(body);
    }

    @GetMapping(value = "/report.md", produces = "text/markdown")
    public ResponseEntity<byte[]> markdownDownload() {
        byte[] body = reports.markdown(reports.report()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sentinel-evaluation-report.md")
                .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8)).body(body);
    }
}
