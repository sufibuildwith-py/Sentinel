package com.sentinel.revenue.economics;

import com.sentinel.revenue.opportunity.OpportunityAction;

import java.time.Instant;
import java.util.List;

public record TimingChannelRecommendation(
        OpportunityAction action,
        Instant recommendedAt,
        String channel,
        String providerWindow,
        String customerEligibility,
        String authorityState,
        EconomicEvidenceQuality evidenceQuality,
        String method,
        List<String> explanation) {
    public TimingChannelRecommendation { explanation = List.copyOf(explanation); }
}
