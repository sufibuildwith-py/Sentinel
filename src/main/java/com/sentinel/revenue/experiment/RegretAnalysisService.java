package com.sentinel.revenue.experiment;

import com.sentinel.revenue.opportunity.OpportunityAction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RegretAnalysisService {
    public RegretAnalysis analyze(UUID incidentId, OpportunityAction actualAction,
                                  BigDecimal actualNetIncrementalValueMinor,
                                  List<RegretCandidate> alternatives,
                                  String policyVersion, String modelVersion) {
        RegretCandidate best = alternatives.stream().filter(RegretCandidate::alreadyPolicyApproved)
                .filter(item -> item.estimatedNetIncrementalValueMinor() != null)
                .max(Comparator.comparing(RegretCandidate::estimatedNetIncrementalValueMinor)
                        .thenComparing(item -> item.action().name())).orElse(null);
        BigDecimal regret = best == null || actualNetIncrementalValueMinor == null ? null
                : best.estimatedNetIncrementalValueMinor().subtract(actualNetIncrementalValueMinor)
                .max(BigDecimal.ZERO);
        return new RegretAnalysis(incidentId, actualAction,
                best == null ? null : best.action(), regret,
                "POST_OUTCOME_APPROVED_ALTERNATIVE_COMPARISON",
                best == null ? "NOT_ESTIMATED" : best.evidenceQuality(),
                "LEARNING_SIGNAL_ONLY_NO_LIVE_OVERRIDE", policyVersion, modelVersion);
    }
}
