package com.sentinel.revenue.economics;

import com.sentinel.revenue.opportunity.CausalMaturity;
import com.sentinel.revenue.opportunity.OpportunityAction;

import java.math.BigDecimal;
import java.time.Instant;

public record CounterfactualEstimate(
        OpportunityAction action,
        CausalMaturity maturity,
        BigDecimal naturalRecoveryProbability,
        BigDecimal actionRecoveryProbability,
        BigDecimal estimatedIncrementalRecoveryMinor,
        BigDecimal estimatedDirectCostMinor,
        BigDecimal estimatedCustomerCostMinor,
        BigDecimal estimatedRiskCostMinor,
        BigDecimal estimatedNetIncrementalValueMinor,
        ProbabilityInterval naturalRecoveryInterval,
        ProbabilityInterval actionRecoveryInterval,
        String method,
        EconomicEvidenceQuality evidenceQuality,
        String modelVersion,
        int naturalSampleSize,
        int actionSampleSize,
        Instant dataWindowStart,
        Instant dataWindowEnd,
        boolean availableAtDecisionTime) { }
