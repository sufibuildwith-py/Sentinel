package com.sentinel.revenue.replay;

import com.sentinel.revenue.governor.KillSwitch;
import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.model.RecoveryStrategy;
import java.util.Set;

public record GovernorReplayContext(long requestedValueMinor, long activeTotalValueMinor,
                                    int activeIncidents, int providerCallsLastMinute,
                                    int customerContacts, int retryCount, int concurrentJobs,
                                    double toolFailureRate, long unreconciledValueMinor,
                                    PolicyDecision policyDecision, RecoveryStrategy strategy,
                                    Set<KillSwitch> enabledKillSwitches) {
    public GovernorReplayContext { enabledKillSwitches = Set.copyOf(enabledKillSwitches); }
}
