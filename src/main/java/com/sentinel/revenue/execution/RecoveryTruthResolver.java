package com.sentinel.revenue.execution;

import com.sentinel.revenue.model.ExecutionMode;
import com.sentinel.revenue.model.RecoveryAction;
import com.sentinel.revenue.model.RecoveryActionStatus;
import com.sentinel.revenue.model.RecoveryLifecycleStage;
import com.sentinel.revenue.model.RecoveryOutcome;
import org.springframework.stereotype.Component;

@Component
public class RecoveryTruthResolver {

    public RecoveryTruth resolve(RecoveryAction action, RecoveryOutcome outcome) {
        if (action == null) {
            return new RecoveryTruth(RecoveryLifecycleStage.PROPOSED,
                    ExecutionMode.LEGACY_UNSPECIFIED, false, false, false, 0,
                    "No recovery action has been created.");
        }
        if (outcome != null && outcome.isProviderConfirmed() && outcome.isTerminalPaid()) {
            return new RecoveryTruth(RecoveryLifecycleStage.RECOVERED_CONFIRMED,
                    action.getExecutionMode(), true, false, true,
                    outcome.getRecoveredAmountMinor(),
                    "A reconciled provider event confirmed the financial outcome.");
        }
        if (outcome != null && outcome.isProviderConfirmed()) {
            return new RecoveryTruth(RecoveryLifecycleStage.AWAITING_RECONCILIATION,
                    action.getExecutionMode(), true, true, true,
                    outcome.getRecoveredAmountMinor(),
                    "Provider-confirmed partial value is recorded; terminal reconciliation is pending.");
        }

        RecoveryActionStatus status = action.getStatus();
        if (status == RecoveryActionStatus.STOPPED && "ACTION_EXPIRED".equals(action.getLastErrorCode())) {
            return terminal(action, RecoveryLifecycleStage.EXPIRED, "The authorized execution window expired.");
        }
        return switch (status) {
            case PROPOSED, PENDING_APPROVAL -> truth(action, RecoveryLifecycleStage.PROPOSED,
                    false, false, "The action remains a proposal without execution authority.");
            case AUTO_APPROVED, APPROVED -> truth(action, RecoveryLifecycleStage.POLICY_APPROVED,
                    false, false, "Deterministic policy or persisted human approval granted authority.");
            case EXECUTING, RETRY_PENDING, EXECUTION_UNCERTAIN -> truth(action,
                    RecoveryLifecycleStage.EXECUTION_REQUESTED, false, false,
                    "Execution was requested but provider acceptance is not a financial outcome.");
            case EXECUTED -> truth(action, RecoveryLifecycleStage.PROVIDER_ACCEPTED,
                    true, true, "The provider accepted a resource; reconciliation is still required.");
            case PARTIALLY_RECOVERED -> truth(action, RecoveryLifecycleStage.AWAITING_RECONCILIATION,
                    true, true, "A partial state exists without a provider-confirmed terminal outcome.");
            case RECOVERED -> truth(action, RecoveryLifecycleStage.AWAITING_RECONCILIATION,
                    true, true, "The action says recovered, but no provider-confirmed outcome proves it.");
            case REJECTED, CANCELLED, FAILED, STOPPED -> terminal(action,
                    RecoveryLifecycleStage.FAILED_CONFIRMED,
                    "The recovery lifecycle ended without provider-confirmed recovered value.");
        };
    }

    private RecoveryTruth truth(RecoveryAction action, RecoveryLifecycleStage stage,
                                boolean providerAccepted, boolean awaitingReconciliation,
                                String basis) {
        return new RecoveryTruth(stage, action.getExecutionMode(), providerAccepted,
                awaitingReconciliation, false, 0, basis);
    }

    private RecoveryTruth terminal(RecoveryAction action, RecoveryLifecycleStage stage, String basis) {
        return truth(action, stage, action.getExternalResourceId() != null, false, basis);
    }
}
