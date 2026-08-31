package com.sentinel.revenue.model;

import java.util.List;

public record RootCauseCandidate(String cause, double confidence,
                                 List<String> support, List<String> contradiction,
                                 String scope) {
    public RootCauseCandidate {
        support = List.copyOf(support);
        contradiction = List.copyOf(contradiction);
    }
}
