package com.sentinel.revenue.model;
import com.sentinel.revenue.policy.PolicyContext;
import com.sentinel.revenue.replay.GovernorReplayContext;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="policy_replay_snapshots")
public class PolicyReplaySnapshot {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="incident_id",nullable=false) private UUID incidentId;
 @Column(name="snapshot_version",nullable=false,length=32) private String snapshotVersion;
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="policy_context",nullable=false,columnDefinition="jsonb") private PolicyContext policyContext;
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="governor_context",nullable=false,columnDefinition="jsonb") private GovernorReplayContext governorContext;
 @Column(name="feature_schema_version",nullable=false,length=32) private String featureSchemaVersion;
 @Column(name="model_version",nullable=false,length=64) private String modelVersion;
 @Column(name="policy_version",nullable=false,length=64) private String policyVersion;
 @Column(name="strategy_version",nullable=false,length=64) private String strategyVersion;
 @Column(name="governor_version",nullable=false,length=64) private String governorVersion;
 @Column(name="replay_seed",nullable=false) private long replaySeed;
 @Enumerated(EnumType.STRING) @Column(name="production_policy_result",nullable=false,length=16) private PolicyDecision productionPolicyResult;
 @Column(name="production_action",nullable=false,length=64) private String productionAction;
 @Column(name="production_governor_result",nullable=false,length=16) private String productionGovernorResult;
 @Column(name="production_predicted_value_minor") private Long productionPredictedValueMinor;
 @Column(name="production_confidence",precision=5,scale=4) private BigDecimal productionConfidence;
 @Column(name="snapshot_sha256",nullable=false,length=64) private String snapshotSha256;
 @Column(name="captured_at",nullable=false) private Instant capturedAt;
 protected PolicyReplaySnapshot(){}
 public PolicyReplaySnapshot(UUID incidentId,PolicyContext context,GovernorReplayContext governorContext,
  String featureSchemaVersion,String modelVersion,String policyVersion,String strategyVersion,String governorVersion,
  long replaySeed,PolicyDecision result,String action,String governorResult,Long predictedValue,BigDecimal confidence,String hash,Instant now){this.incidentId=incidentId;this.snapshotVersion="policy-replay-v1";this.policyContext=context;this.governorContext=governorContext;this.featureSchemaVersion=featureSchemaVersion;this.modelVersion=modelVersion;this.policyVersion=policyVersion;this.strategyVersion=strategyVersion;this.governorVersion=governorVersion;this.replaySeed=replaySeed;this.productionPolicyResult=result;this.productionAction=action;this.productionGovernorResult=governorResult;this.productionPredictedValueMinor=predictedValue;this.productionConfidence=confidence;this.snapshotSha256=hash;this.capturedAt=now;}
 public UUID getId(){return id;} public UUID getIncidentId(){return incidentId;} public PolicyContext getPolicyContext(){return policyContext;} public GovernorReplayContext getGovernorContext(){return governorContext;} public PolicyDecision getProductionPolicyResult(){return productionPolicyResult;} public String getProductionAction(){return productionAction;} public String getProductionGovernorResult(){return productionGovernorResult;} public Long getProductionPredictedValueMinor(){return productionPredictedValueMinor;} public BigDecimal getProductionConfidence(){return productionConfidence;} public String getSnapshotSha256(){return snapshotSha256;} public String getFeatureSchemaVersion(){return featureSchemaVersion;} public String getModelVersion(){return modelVersion;} public String getPolicyVersion(){return policyVersion;} public String getStrategyVersion(){return strategyVersion;} public String getGovernorVersion(){return governorVersion;} public long getReplaySeed(){return replaySeed;} public Instant getCapturedAt(){return capturedAt;}
}
