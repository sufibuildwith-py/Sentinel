package com.sentinel.revenue.repository;
import com.sentinel.revenue.model.ModelPromotionApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ModelPromotionApprovalRepository extends JpaRepository<ModelPromotionApproval, UUID> { }
