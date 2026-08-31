package com.sentinel.revenue.experiment;

import java.util.List;
import java.util.UUID;

public record ExperimentDefinition(UUID experimentId, String name, long seed,
                                   List<ExperimentArm> arms, long maximumExposureMinor,
                                   int minimumSampleSizePerArm, double maximumHarmRate,
                                   String merchantApprovalReference, String policyVersion,
                                   String modelVersion) {
    public ExperimentDefinition { arms = List.copyOf(arms); }
}
