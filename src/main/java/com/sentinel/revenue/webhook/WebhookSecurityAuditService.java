package com.sentinel.revenue.webhook;

import com.sentinel.revenue.model.WebhookSecurityEvent;
import com.sentinel.revenue.repository.WebhookSecurityEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class WebhookSecurityAuditService {
    private final WebhookSecurityEventRepository repository;
    public WebhookSecurityAuditService(WebhookSecurityEventRepository repository) { this.repository = repository; }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String digest, boolean signaturePresent, boolean eventIdPresent, String reason) {
        repository.append(new WebhookSecurityEvent(Instant.now(), digest, signaturePresent, eventIdPresent, reason));
    }
}
