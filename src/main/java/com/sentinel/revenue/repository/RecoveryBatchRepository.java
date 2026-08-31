package com.sentinel.revenue.repository;
import com.sentinel.revenue.model.RecoveryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface RecoveryBatchRepository extends JpaRepository<RecoveryBatch, UUID> { }
