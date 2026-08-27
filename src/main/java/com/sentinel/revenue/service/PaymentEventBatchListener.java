package com.sentinel.revenue.service;

import com.sentinel.revenue.model.PaymentEvent;

import java.util.List;

public interface PaymentEventBatchListener {
    void onEventsPersisted(List<PaymentEvent> events);
}
