package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.SystemicIncidentMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface SystemicIncidentMemberRepository extends JpaRepository<SystemicIncidentMember, UUID> {
    @Query("select member from SystemicIncidentMember member "
            + "where member.systemicIncident.id = :systemicIncidentId")
    List<SystemicIncidentMember> findAllBySystemicIncidentId(
            @Param("systemicIncidentId") UUID systemicIncidentId);
    boolean existsByPaymentIncidentIncidentId(UUID paymentIncidentId);
}
