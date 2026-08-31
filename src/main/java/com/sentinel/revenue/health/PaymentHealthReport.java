package com.sentinel.revenue.health;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PaymentHealthReport(String merchantId, Instant evaluatedAt,
                                  Map<String, PaymentHealthWindow> current,
                                  Map<String, PaymentHealthWindow> baseline,
                                  List<PaymentHealthSignal> signals) {
    public PaymentHealthReport {
        current = Map.copyOf(current);
        baseline = Map.copyOf(baseline);
        signals = List.copyOf(signals);
    }
}
