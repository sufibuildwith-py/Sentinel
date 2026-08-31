package com.sentinel.revenue.governor;

import com.sentinel.revenue.model.RecoveryKillSwitch;
import com.sentinel.revenue.repository.RecoveryKillSwitchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class KillSwitchService {
    private final RecoveryKillSwitchRepository switches;
    public KillSwitchService(RecoveryKillSwitchRepository switches) { this.switches = switches; }
    @Transactional(readOnly = true)
    public boolean enabled(KillSwitch name) { return switches.findById(name).map(RecoveryKillSwitch::isEnabled).orElse(false); }
    @Transactional
    public void set(KillSwitch name, boolean enabled, String reason) {
        RecoveryKillSwitch state = switches.findById(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown kill switch: " + name));
        state.set(enabled, reason, Instant.now()); switches.saveAndFlush(state);
    }
    @Transactional(readOnly = true)
    public Map<KillSwitch, Boolean> states() {
        Map<KillSwitch, Boolean> result = new EnumMap<>(KillSwitch.class);
        for (KillSwitch value : KillSwitch.values()) result.put(value, enabled(value));
        return Map.copyOf(result);
    }
}
