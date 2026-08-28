package com.sentinel.revenue.webhook;

public record WebhookResult(String eventId, String disposition, boolean duplicate,
                            String message) { }
