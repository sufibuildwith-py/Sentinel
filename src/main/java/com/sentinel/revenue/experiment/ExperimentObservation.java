package com.sentinel.revenue.experiment;

import java.time.Duration;
import java.util.UUID;

public record ExperimentObservation(UUID incidentId, String arm, boolean control,
                                    boolean providerConfirmedRecovery, long recoveredAmountMinor,
                                    long recoveryCostMinor, Duration timeToRecovery,
                                    boolean customerResponse, boolean harmSignal) { }
