package com.sentinel.revenue.portfolio;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class HumanReviewOptimizer {
    public List<HumanReviewCandidate> rank(List<HumanReviewCandidate> candidates, int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("capacity cannot be negative");
        return candidates.stream().sorted(Comparator
                        .comparing(this::valuePerMinute, Comparator.reverseOrder())
                        .thenComparing(HumanReviewCandidate::decisionDeadline)
                        .thenComparing(candidate -> candidate.incidentId().toString()))
                .limit(capacity).toList();
    }

    private BigDecimal valuePerMinute(HumanReviewCandidate candidate) {
        if (candidate.expectedIncrementalValueMinor() == null || candidate.estimatedReviewMinutes() <= 0) {
            return BigDecimal.ZERO;
        }
        return candidate.expectedIncrementalValueMinor().divide(
                BigDecimal.valueOf(candidate.estimatedReviewMinutes()), 4, RoundingMode.HALF_UP);
    }
}
