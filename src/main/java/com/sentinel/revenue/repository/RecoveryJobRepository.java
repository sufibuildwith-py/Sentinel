package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.RecoveryJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecoveryJobRepository extends JpaRepository<RecoveryJob, UUID> {
    List<RecoveryJob> findByStatusAndNextAttemptAtBefore(String status, Instant now);
    Optional<RecoveryJob> findByIncidentId(UUID incidentId);
}
