package com.sentinel.revenue.replay;
import java.math.BigDecimal;
import java.util.List;
public record ShadowCandidate(String action,String modelVersion,ReplayPolicyVersion policyVersion,Long predictedValueMinor,BigDecimal confidence,BigDecimal priority,List<String> opportunityRanking,String explanation){public ShadowCandidate{opportunityRanking=List.copyOf(opportunityRanking);}}
