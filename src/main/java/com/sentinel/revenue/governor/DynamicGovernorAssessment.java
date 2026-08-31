package com.sentinel.revenue.governor;

import java.util.List;

public record DynamicGovernorAssessment(GovernorPosture posture, double authorityMultiplier,
                                        List<String> evidence, String evaluatorVersion) {
    public DynamicGovernorAssessment { evidence = List.copyOf(evidence); }
}
