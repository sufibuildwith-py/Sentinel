package com.sentinel.revenue.service;

import com.sentinel.revenue.model.RecoveryJob;
import com.sentinel.revenue.repository.RecoveryJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Set;

@Service
public class RecoveryJobService {

    private final RecoveryJobRepository repository;

    public RecoveryJobService(RecoveryJobRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RecoveryJob createJob(UUID incidentId, UUID policyDecisionId, String strategy) {
        return repository.saveAndFlush(new RecoveryJob(
                incidentId, policyDecisionId, strategy, 3, Instant.now()));
    }

    @Transactional
    public RecoveryJob createJobIfAbsent(UUID incidentId, UUID policyDecisionId, String strategy) {
        return repository.findFirstByIncidentIdAndStrategyAndStatusInOrderByCreatedAtDesc(
                        incidentId, strategy, Set.of(RecoveryJob.PENDING, RecoveryJob.RUNNING))
                .orElseGet(() -> repository.saveAndFlush(new RecoveryJob(
                        incidentId, policyDecisionId, strategy, 3, Instant.now())));
    }

    @Transactional(readOnly = true)
    public RecoveryJob get(UUID jobId) {
        return find(jobId);
    }

    @Transactional
    public RecoveryJob markRunning(UUID jobId) {
        RecoveryJob job = repository.findForUpdateById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Recovery job not found: " + jobId));
        job.markRunning(Instant.now());
        return repository.saveAndFlush(job);
    }

    @Transactional
    public RecoveryJob markSucceeded(UUID jobId) {
        RecoveryJob job = find(jobId);
        job.markSucceeded(Instant.now());
        return repository.saveAndFlush(job);
    }

    @Transactional
    public RecoveryJob markFailed(UUID jobId, String errorDetail) {
        RecoveryJob job = find(jobId);
        job.markFailed(errorDetail, Instant.now());
        return repository.saveAndFlush(job);
    }

    @Transactional
    public RecoveryJob markExhausted(UUID jobId) {
        RecoveryJob job = find(jobId);
        job.markExhausted(Instant.now());
        return repository.saveAndFlush(job);
    }

    @Transactional(readOnly = true)
    public List<RecoveryJob> findPendingDueJobs() {
        return repository.findByStatusAndNextAttemptAtBefore(RecoveryJob.PENDING, Instant.now());
    }

    private RecoveryJob find(UUID jobId) {
        return repository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Recovery job not found: " + jobId));
    }
}
