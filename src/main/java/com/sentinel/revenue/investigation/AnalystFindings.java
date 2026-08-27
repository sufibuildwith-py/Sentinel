package com.sentinel.revenue.investigation;

import java.util.List;

public record AnalystFindings(PatternAnalysis pattern, CustomerContext customerContext,
                              List<String> evidence, double confidence) {
    public AnalystFindings { evidence = List.copyOf(evidence); }
}
