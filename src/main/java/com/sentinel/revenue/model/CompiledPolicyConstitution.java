package com.sentinel.revenue.model;

import com.sentinel.revenue.policy.MerchantPolicyConstitution;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compiled_policy_constitutions")
public class CompiledPolicyConstitution {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "policy_id", nullable = false) private UUID policyId;
    @Column(name = "merchant_id", nullable = false, length = 128) private String merchantId;
    @Column(name = "policy_version", nullable = false, length = 64) private String policyVersion;
    @Column(name = "compiler_version", nullable = false, length = 64) private String compilerVersion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb")
    private MerchantPolicyConstitution constitution;
    @Column(name = "constitution_sha256", nullable = false, length = 64) private String constitutionSha256;
    @Column(name = "effective_at") private Instant effectiveAt;
    @Column(name = "approval_reference", length = 128) private String approvalReference;
    @Column(name = "benchmark_reference", length = 128) private String benchmarkReference;
    @Column(name = "replay_reference", length = 128) private String replayReference;
    @Column(name = "shadow_reference", length = 128) private String shadowReference;
    @Column(nullable = false, length = 64) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected CompiledPolicyConstitution() { }
    public CompiledPolicyConstitution(UUID policyId, String merchantId, String policyVersion,
                                      String compilerVersion, MerchantPolicyConstitution constitution,
                                      String hash, Instant effectiveAt, String approvalReference,
                                      String benchmarkReference, String replayReference,
                                      String shadowReference, String status, Instant createdAt) {
        this.policyId = policyId; this.merchantId = merchantId; this.policyVersion = policyVersion;
        this.compilerVersion = compilerVersion; this.constitution = constitution;
        this.constitutionSha256 = hash; this.effectiveAt = effectiveAt;
        this.approvalReference = approvalReference; this.benchmarkReference = benchmarkReference;
        this.replayReference = replayReference; this.shadowReference = shadowReference;
        this.status = status; this.createdAt = createdAt;
    }
    public UUID getId() { return id; } public UUID getPolicyId() { return policyId; }
    public String getMerchantId() { return merchantId; } public String getPolicyVersion() { return policyVersion; }
    public String getCompilerVersion() { return compilerVersion; }
    public MerchantPolicyConstitution getConstitution() { return constitution; }
    public String getConstitutionSha256() { return constitutionSha256; }
    public Instant getEffectiveAt() { return effectiveAt; } public String getApprovalReference() { return approvalReference; }
    public String getBenchmarkReference() { return benchmarkReference; } public String getReplayReference() { return replayReference; }
    public String getShadowReference() { return shadowReference; } public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
