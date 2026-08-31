package com.sentinel.revenue.replay;
import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.policy.PolicyRuleResult;
import java.util.*;
public record PolicyReplayResult(UUID snapshotId,String snapshotHash,long seed,String featureSchemaVersion,String modelVersion,String policyVersion,String strategyVersion,String governorVersion,PolicyDecision policyDecision,String governorDisposition,long allowedValueMinor,List<PolicyRuleResult> ruleTrace,List<String> governorViolations,String evaluatedAt){public PolicyReplayResult{ruleTrace=List.copyOf(ruleTrace);governorViolations=List.copyOf(governorViolations);}}
