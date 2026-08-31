package com.sentinel.revenue.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "policy_change_proposals")
public class PolicyChangeProposal {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "proposed_by_model_id") private UUID proposedByModelId;
    @Column(name = "policy_version", nullable = false, length = 64) private String policyVersion;
    @Column(nullable = false, columnDefinition = "text") private String proposal;
    @Column(nullable = false, length = 32) private String status;
    @Column(name = "replay_passed", nullable = false) private boolean replayPassed;
    @Column(name = "shadow_passed", nullable = false) private boolean shadowPassed;
    @Column(name = "approved_by", length = 128) private String approvedBy;
    @Column(name = "approval_reason", columnDefinition = "text") private String approvalReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "approved_at") private Instant approvedAt;
    protected PolicyChangeProposal() { }
    public PolicyChangeProposal(UUID modelId, String policyVersion, String proposal, Instant now) {
        this.proposedByModelId=modelId; this.policyVersion=policyVersion; this.proposal=proposal;
        this.status="PROPOSED"; this.createdAt=now;
    }
    public void recordReplay(boolean passed) { replayPassed=passed; status=passed?"REPLAY_PASSED":"REPLAY_FAILED"; }
    public void recordShadow(boolean passed) {
        if (!replayPassed) throw new IllegalStateException("Replay must pass before shadow");
        shadowPassed=passed; status=passed?"SHADOW_PASSED":"SHADOW_FAILED";
    }
    public void approve(String actor, String reason, Instant now) {
        if (!replayPassed || !shadowPassed) throw new IllegalStateException("Replay and shadow must pass before approval");
        if (actor == null || actor.isBlank() || reason == null || reason.isBlank()) throw new IllegalArgumentException("Human actor and reason required");
        approvedBy=actor; approvalReason=reason; approvedAt=now; status="APPROVED_FOR_VERSIONED_DEPLOYMENT";
    }
    public UUID getId(){return id;} public String getStatus(){return status;}
}
