package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.ProcessedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, UUID> {
    boolean existsByEventId(String eventId);

    Optional<ProcessedWebhookEvent> findByEventId(String eventId);
}
