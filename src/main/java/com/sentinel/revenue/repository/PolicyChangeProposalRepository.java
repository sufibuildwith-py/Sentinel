package com.sentinel.revenue.repository;
import com.sentinel.revenue.model.PolicyChangeProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface PolicyChangeProposalRepository extends JpaRepository<PolicyChangeProposal, UUID> { }
