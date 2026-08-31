package com.sentinel.revenue.governor;
import org.springframework.stereotype.Service;

@Service
public class GovernorCircuitBreakerService {
    private final KillSwitchService killSwitches;
    public GovernorCircuitBreakerService(KillSwitchService killSwitches) { this.killSwitches = killSwitches; }
    public void trip(String trigger) {
        switch (trigger) {
            case "PROVIDER_5XX_SPIKE", "PROVIDER_TIMEOUT_SPIKE", "RECONCILIATION_MISMATCH",
                 "DUPLICATE_EXECUTION_ANOMALY", "POLICY_VIOLATION", "EXCESSIVE_UNRECONCILED_EXPOSURE",
                 "NEW_SYSTEMIC_DOWNTIME", "RECOVERY_OUTCOME_COLLAPSE" ->
                    killSwitches.set(KillSwitch.ALL_AUTONOMOUS_EXECUTION, true, trigger);
            default -> throw new IllegalArgumentException("Unsupported circuit-breaker trigger: " + trigger);
        }
    }
}
