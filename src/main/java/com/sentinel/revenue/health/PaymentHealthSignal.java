package com.sentinel.revenue.health;

import java.util.List;

public record PaymentHealthSignal(PaymentHealthSignalType type, boolean active,
                                  double actual, double baseline, double threshold,
                                  String scope, List<String> evidence) {
    public PaymentHealthSignal { evidence = List.copyOf(evidence); }
}
