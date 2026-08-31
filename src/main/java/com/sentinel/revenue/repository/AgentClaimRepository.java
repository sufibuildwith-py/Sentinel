package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.AgentClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentClaimRepository extends JpaRepository<AgentClaim, UUID> {
    List<AgentClaim> findAllByIncidentIncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
