package com.sentinel.revenue.experiment;

import java.util.UUID;

public record ExperimentCandidate(UUID incidentId, long amountAtRiskMinor,
                                  boolean policyEligible, boolean governorEligible,
                                  boolean harmSignalPresent) { }
