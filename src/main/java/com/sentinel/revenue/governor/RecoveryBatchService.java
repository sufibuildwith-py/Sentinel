package com.sentinel.revenue.governor;

import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class RecoveryBatchService {
    private final RecoveryBatchRepository batches;
    private final RecoveryActionRepository actions;
    private final RecoveryOutcomeRepository outcomes;
    private final RecoverySafetyProperties properties;
    public RecoveryBatchService(RecoveryBatchRepository batches, RecoveryActionRepository actions,
                                RecoveryOutcomeRepository outcomes, RecoverySafetyProperties properties) {
        this.batches = batches; this.actions = actions; this.outcomes = outcomes; this.properties = properties;
    }
    @Transactional
    public RecoveryBatch create(String strategy, List<UUID> incidentIds) {
        if (incidentIds.isEmpty()) throw new IllegalArgumentException("Recovery batch cannot be empty");
        return batches.saveAndFlush(new RecoveryBatch(strategy, incidentIds, properties.canarySize(),
                properties.requiredReconciledCount(), Instant.now()));
    }
    @Transactional
    public RecoveryBatch expandAfterReconciliation(UUID batchId) {
        RecoveryBatch batch = batches.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Recovery batch not found: " + batchId));
        List<UUID> released = batch.getIncidentIds().subList(0, batch.getReleasedCount());
        long confirmed = released.stream().map(actions::findFirstByIncidentIncidentIdOrderByCreatedAtDesc)
                .flatMap(Optional::stream).map(action -> outcomes.findByRecoveryActionId(action.getId()))
                .flatMap(Optional::stream).filter(RecoveryOutcome::isProviderConfirmed).count();
        if (confirmed < batch.getRequiredReconciledCount())
            throw new IllegalStateException("Canary expansion requires reconciled outcomes: confirmed="
                    + confirmed + " required=" + batch.getRequiredReconciledCount());
        batch.expand(batch.getCanarySize(), Instant.now());
        return batches.saveAndFlush(batch);
    }
}
