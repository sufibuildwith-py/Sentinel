package com.sentinel.revenue.repository;

import com.sentinel.revenue.model.CompiledPolicyConstitution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompiledPolicyConstitutionRepository extends JpaRepository<CompiledPolicyConstitution, UUID> {
    Optional<CompiledPolicyConstitution> findByMerchantIdAndPolicyVersion(String merchantId, String policyVersion);
    List<CompiledPolicyConstitution> findAllByMerchantIdOrderByCreatedAtDesc(String merchantId);
}
