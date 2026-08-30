package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    Optional<WebhookEvent> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
    List<WebhookEvent> findByProcessedFalseOrderByReceivedAtAsc();
}
