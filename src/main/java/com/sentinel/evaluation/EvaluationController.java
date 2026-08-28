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

    public EvaluationController(EvaluationReportService reports) { this.reports = reports; }

    @GetMapping("/report")
    public EvaluationReport report() { return reports.report(); }

    @PostMapping("/run")
    public EvaluationReport run() {
        reports.runAndPersist();
        return reports.report();
    }

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
