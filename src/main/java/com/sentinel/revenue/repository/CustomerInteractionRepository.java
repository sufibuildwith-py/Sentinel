package com.sentinel.revenue.repository;
import com.sentinel.revenue.model.CustomerInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.*;
public interface CustomerInteractionRepository extends JpaRepository<CustomerInteraction, UUID> {
    long countByCustomerRefAndCreatedAtAfter(String customerRef, Instant after);
    List<CustomerInteraction> findAllByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
    List<CustomerInteraction> findAllByCustomerRefOrderByCreatedAtAsc(String customerRef);
}
