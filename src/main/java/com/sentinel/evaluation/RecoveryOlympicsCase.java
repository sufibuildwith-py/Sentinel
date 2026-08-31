package com.sentinel.evaluation;

public record RecoveryOlympicsCase(
        int sequence,
        RecoveryOlympicsSplit split,
        long amountMinor,
        boolean naturallyRecovers,
        boolean treatmentWouldRecover,
        boolean alreadyPaid,
        boolean duplicateRisk,
        boolean unacceptableRisk,
        boolean contactAllowed,
        boolean providerHealthy,
        double predictedRecoveryProbability,
        long interventionCostMinor,
        long naturalRecoveryMinutes,
        long treatmentRecoveryMinutes) {
}
