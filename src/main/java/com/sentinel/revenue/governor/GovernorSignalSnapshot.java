package com.sentinel.revenue.governor;

public record GovernorSignalSnapshot(double toolFailureRate, long unreconciledValueMinor,
                                     long activeExposureMinor, long maximumUnreconciledValueMinor,
                                     long maximumExposureMinor) { }
