package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.IncidentFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import java.util.UUID;

public interface IncidentFindingRepository extends JpaRepository<IncidentFinding, UUID> {
    List<IncidentFinding> findAllByIncidentIncidentId(UUID incidentId);

    void deleteAllByIncidentIncidentIdIn(Collection<UUID> incidentIds);
}
