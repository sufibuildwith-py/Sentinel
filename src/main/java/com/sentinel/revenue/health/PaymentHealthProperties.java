package com.sentinel.revenue.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentinel.payment-health")
public record PaymentHealthProperties(int minimumVolume, double successRateDrop,
                                      double failureRateIncrease,
                                      double failureVelocityMultiplier,
                                      double concentrationThreshold,
                                      long revenueExposureMinor,
                                      double recoveryToolErrorRate) { }
