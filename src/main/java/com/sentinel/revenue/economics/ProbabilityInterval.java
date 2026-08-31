package com.sentinel.revenue.economics;

import java.math.BigDecimal;

public record ProbabilityInterval(BigDecimal lower, BigDecimal upper, String method) { }
