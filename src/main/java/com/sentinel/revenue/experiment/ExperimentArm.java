package com.sentinel.revenue.experiment;

import com.sentinel.revenue.opportunity.OpportunityAction;

public record ExperimentArm(String name, OpportunityAction action, int allocationPercent,
                            boolean control, String strategyVersion) { }
