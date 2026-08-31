package com.sentinel.revenue.governor;

public record ExecutionEnvelope(long maxTotalValueMinor, int maxIncidents,
                                long maxValuePerIncidentMinor, int maxProviderCallsPerMinute,
                                int maxCustomerContacts, int maxRetryCount,
                                int maxConcurrentJobs, double maxToolFailureRate,
                                long maxUnreconciledValueMinor) { }
