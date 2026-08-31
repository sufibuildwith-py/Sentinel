package com.sentinel.revenue.communication;
public interface CommunicationAdapter {
    CommunicationResult send(String customerReference, String channel, String approvedTemplateId);
    String mode();
}
