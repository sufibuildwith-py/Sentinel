package com.sentinel.revenue.repository;

import com.sentinel.revenue.governor.KillSwitch;
import com.sentinel.revenue.model.RecoveryKillSwitch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryKillSwitchRepository extends JpaRepository<RecoveryKillSwitch, KillSwitch> { }
