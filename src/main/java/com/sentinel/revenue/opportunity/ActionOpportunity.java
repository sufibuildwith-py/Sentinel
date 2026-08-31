package com.sentinel.revenue.opportunity;
import java.math.BigDecimal;
import java.util.List;
public record ActionOpportunity(OpportunityAction action, BigDecimal priorityScore,
                                BigDecimal naturalRecoveryProbability,
                                BigDecimal actionRecoveryProbability,
                                BigDecimal incrementalUplift, Long netIncrementalValueMinor,
                                String policyState, String governorState, String estimateKind,
                                List<String> explanation) {
    public ActionOpportunity { explanation = List.copyOf(explanation); }
}
