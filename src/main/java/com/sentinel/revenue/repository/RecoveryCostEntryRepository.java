package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.RecoveryCostEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecoveryCostEntryRepository extends JpaRepository<RecoveryCostEntry, UUID> {
    List<RecoveryCostEntry> findAllByIncidentIdOrderByOccurredAtAsc(UUID incidentId);
}
