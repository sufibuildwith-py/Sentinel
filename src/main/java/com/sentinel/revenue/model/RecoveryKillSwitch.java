package com.sentinel.revenue.model;

import com.sentinel.revenue.governor.KillSwitch;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "recovery_kill_switches")
public class RecoveryKillSwitch {
    @Id @Enumerated(EnumType.STRING)
    @Column(name = "switch_name", length = 64)
    private KillSwitch switchName;
    @Column(nullable = false)
    private boolean enabled;
    @Column(columnDefinition = "text")
    private String reason;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    protected RecoveryKillSwitch() { }
    public KillSwitch getSwitchName() { return switchName; }
    public boolean isEnabled() { return enabled; }
    public String getReason() { return reason; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void set(boolean enabled, String reason, Instant now) {
        this.enabled = enabled; this.reason = reason; this.updatedAt = now;
    }
}
