package com.sentinel.revenue.failurelab;

public record FailureLabScenario(String id, String title, String description,
                                 FailureLabMode mode, String expectedSafetyOutcome,
                                 String evidenceSelector, boolean runnable) { }
