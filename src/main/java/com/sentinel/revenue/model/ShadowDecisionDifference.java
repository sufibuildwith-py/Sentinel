package com.sentinel.revenue.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="shadow_decision_differences")
public class ShadowDecisionDifference {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="snapshot_id",nullable=false) private UUID snapshotId;
 @Column(name="production_action",nullable=false,length=64) private String productionAction;
 @Column(name="shadow_action",nullable=false,length=64) private String shadowAction;
 @Enumerated(EnumType.STRING) @Column(name="production_policy_result",nullable=false,length=16) private PolicyDecision productionPolicyResult;
 @Enumerated(EnumType.STRING) @Column(name="shadow_policy_result",nullable=false,length=16) private PolicyDecision shadowPolicyResult;
 @Column(name="production_governor_result",nullable=false,length=16) private String productionGovernorResult;
 @Column(name="shadow_governor_result",nullable=false,length=16) private String shadowGovernorResult;
 @Column(name="production_predicted_value_minor") private Long productionPredictedValueMinor;
 @Column(name="shadow_predicted_value_minor") private Long shadowPredictedValueMinor;
 @Column(name="production_confidence",precision=5,scale=4) private BigDecimal productionConfidence;
 @Column(name="shadow_confidence",precision=5,scale=4) private BigDecimal shadowConfidence;
 @Column(name="production_priority",precision=8,scale=4) private BigDecimal productionPriority;
 @Column(name="shadow_priority",precision=8,scale=4) private BigDecimal shadowPriority;
 @Column(name="opportunity_ranking_changed",nullable=false) private boolean opportunityRankingChanged;
 @Column(name="approval_requirement_changed",nullable=false) private boolean approvalRequirementChanged;
 @Column(nullable=false,columnDefinition="text") private String explanation;
 @Column(name="critical_regression",nullable=false) private boolean criticalRegression;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 protected ShadowDecisionDifference(){}
 public ShadowDecisionDifference(UUID snapshotId,String productionAction,String shadowAction,PolicyDecision productionPolicyResult,PolicyDecision shadowPolicyResult,String productionGovernorResult,String shadowGovernorResult,Long productionValue,Long shadowValue,BigDecimal productionConfidence,BigDecimal shadowConfidence,BigDecimal productionPriority,BigDecimal shadowPriority,boolean rankingChanged,boolean approvalChanged,String explanation,boolean criticalRegression,Instant now){this.snapshotId=snapshotId;this.productionAction=productionAction;this.shadowAction=shadowAction;this.productionPolicyResult=productionPolicyResult;this.shadowPolicyResult=shadowPolicyResult;this.productionGovernorResult=productionGovernorResult;this.shadowGovernorResult=shadowGovernorResult;this.productionPredictedValueMinor=productionValue;this.shadowPredictedValueMinor=shadowValue;this.productionConfidence=productionConfidence;this.shadowConfidence=shadowConfidence;this.productionPriority=productionPriority;this.shadowPriority=shadowPriority;this.opportunityRankingChanged=rankingChanged;this.approvalRequirementChanged=approvalChanged;this.explanation=explanation;this.criticalRegression=criticalRegression;this.createdAt=now;}
 public UUID getId(){return id;} public UUID getSnapshotId(){return snapshotId;} public boolean isCriticalRegression(){return criticalRegression;} public String getProductionAction(){return productionAction;} public String getShadowAction(){return shadowAction;} public PolicyDecision getProductionPolicyResult(){return productionPolicyResult;} public PolicyDecision getShadowPolicyResult(){return shadowPolicyResult;} public String getProductionGovernorResult(){return productionGovernorResult;} public String getShadowGovernorResult(){return shadowGovernorResult;} public boolean isOpportunityRankingChanged(){return opportunityRankingChanged;} public boolean isApprovalRequirementChanged(){return approvalRequirementChanged;} public String getExplanation(){return explanation;} public Instant getCreatedAt(){return createdAt;}
}
