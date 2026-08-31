package com.sentinel.revenue.governor;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentinel.governor")
public record RecoverySafetyProperties(long maxTotalValueMinor, int maxIncidents,
                                       long maxValuePerIncidentMinor,
                                       int maxProviderCallsPerMinute,
                                       int maxCustomerContacts, int maxRetryCount,
                                       int maxConcurrentJobs, double maxToolFailureRate,
                                       long maxUnreconciledValueMinor,
                                       int canarySize, int requiredReconciledCount) { }
