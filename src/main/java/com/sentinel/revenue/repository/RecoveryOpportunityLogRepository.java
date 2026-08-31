package com.sentinel.revenue.repository;
import com.sentinel.revenue.model.RecoveryOpportunityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RecoveryOpportunityLogRepository extends JpaRepository<RecoveryOpportunityLog, UUID> {
    List<RecoveryOpportunityLog> findAllByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
