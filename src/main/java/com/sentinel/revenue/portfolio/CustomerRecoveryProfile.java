package com.sentinel.revenue.portfolio;

import com.sentinel.revenue.economics.EconomicEvidenceQuality;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CustomerRecoveryProfile(UUID incidentId, int customerCount, int paymentSamples,
                                      int successfulPayments, String preferredPaymentRail,
                                      Map<String, Long> failureClasses,
                                      Map<Integer, Long> successfulUtcHours,
                                      int priorInteractions, int simulatedInteractions,
                                      int promisesObserved, int promisesKept,
                                      BigDecimal promiseReliability,
                                      EconomicEvidenceQuality evidenceQuality,
                                      String featureSchemaVersion,
                                      List<FeatureDefinition> featureDefinitions) {
    public CustomerRecoveryProfile {
        failureClasses = Map.copyOf(failureClasses);
        successfulUtcHours = Map.copyOf(successfulUtcHours);
        featureDefinitions = List.copyOf(featureDefinitions);
    }

    public record FeatureDefinition(String name, String source, String purpose,
                                    String retention, String allowedUse) { }
}
