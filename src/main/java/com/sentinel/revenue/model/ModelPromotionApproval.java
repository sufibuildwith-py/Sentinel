package com.sentinel.revenue.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "model_promotion_approvals")
public class ModelPromotionApproval {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "model_id", nullable = false) private UUID modelId;
    @Enumerated(EnumType.STRING) @Column(name = "from_lifecycle", nullable = false, length = 16) private ModelLifecycle from;
    @Enumerated(EnumType.STRING) @Column(name = "to_lifecycle", nullable = false, length = 16) private ModelLifecycle to;
    @Column(nullable = false, length = 128) private String actor;
    @Column(nullable = false, columnDefinition = "text") private String reason;
    @Column(name = "evaluation_report_version", nullable = false, length = 64) private String evaluationReportVersion;
    @Column(name = "evaluation_seed", nullable = false) private long evaluationSeed;
    @Column(name = "approved_at", nullable = false) private Instant approvedAt;
    protected ModelPromotionApproval() { }
    public ModelPromotionApproval(UUID modelId, ModelLifecycle from, ModelLifecycle to, String actor,
                                  String reason, String report, long seed, Instant now) {
        this.modelId=modelId; this.from=from; this.to=to; this.actor=actor; this.reason=reason;
        this.evaluationReportVersion=report; this.evaluationSeed=seed; this.approvedAt=now;
    }
}
