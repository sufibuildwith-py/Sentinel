package com.sentinel.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, UUID> {
    Optional<EvaluationRun> findFirstByOrderByCreatedAtDesc();
    Optional<EvaluationRun> findByReportVersionAndSeedAndReportSha256(
            String reportVersion, long seed, String reportSha256);
}
