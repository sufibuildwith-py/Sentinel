package com.sentinel.revenue.health;

import java.time.Duration;
import java.util.Map;

public record PaymentHealthWindow(Duration duration, int volume, int failures,
                                  long amountAtRiskMinor, double successRate,
                                  double failureVelocityPerMinute,
                                  Map<String, Double> methodSuccessRates,
                                  Map<String, Long> bankFailures,
                                  Map<String, Long> errorFailures,
                                  Map<Integer, Long> hourOfDay,
                                  Map<String, Long> dayOfWeek) { }
