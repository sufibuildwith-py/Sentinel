package com.sentinel.revenue.experiment;

import com.sentinel.revenue.opportunity.OpportunityAction;

import java.util.UUID;

public record ExperimentAssignment(UUID experimentId, UUID incidentId, String arm,
                                   OpportunityAction action, boolean control,
                                   String disposition, long allocatedExposureMinor,
                                   String authorityState) { }
