package com.sentinel.evaluation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evaluation_runs")
public class EvaluationRun {
    @Id
    private UUID id;
    @Column(name = "report_version", nullable = false, length = 32)
    private String reportVersion;
    @Column(nullable = false)
    private long seed;
    @Column(name = "dataset_size", nullable = false)
    private int datasetSize;
    @Column(name = "report_sha256", nullable = false, length = 64)
    private String reportSha256;
    @Column(name = "report_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String reportJson;
    @Column(name = "report_markdown", nullable = false, columnDefinition = "text")
    private String reportMarkdown;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EvaluationRun() { }

    public EvaluationRun(UUID id, String reportVersion, long seed, int datasetSize,
                         String reportSha256, String reportJson, String reportMarkdown,
                         Instant createdAt) {
        this.id = id;
        this.reportVersion = reportVersion;
        this.seed = seed;
        this.datasetSize = datasetSize;
        this.reportSha256 = reportSha256;
        this.reportJson = reportJson;
        this.reportMarkdown = reportMarkdown;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getReportVersion() { return reportVersion; }
    public long getSeed() { return seed; }
    public int getDatasetSize() { return datasetSize; }
    public String getReportSha256() { return reportSha256; }
    public String getReportJson() { return reportJson; }
    public String getReportMarkdown() { return reportMarkdown; }
    public Instant getCreatedAt() { return createdAt; }
}
