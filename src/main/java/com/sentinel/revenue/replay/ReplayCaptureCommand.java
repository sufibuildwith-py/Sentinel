package com.sentinel.revenue.replay;
import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.policy.PolicyContext;
import java.math.BigDecimal;
import java.util.UUID;
public record ReplayCaptureCommand(UUID incidentId,PolicyContext policyContext,GovernorReplayContext governorContext,String featureSchemaVersion,String modelVersion,String policyVersion,String strategyVersion,String governorVersion,long seed,PolicyDecision productionPolicyResult,String productionAction,String productionGovernorResult,Long productionPredictedValueMinor,BigDecimal productionConfidence){}
