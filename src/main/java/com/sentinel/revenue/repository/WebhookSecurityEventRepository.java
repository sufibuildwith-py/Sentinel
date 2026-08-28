package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.WebhookSecurityEvent;

public interface WebhookSecurityEventRepository {
    WebhookSecurityEvent append(WebhookSecurityEvent event);
}
