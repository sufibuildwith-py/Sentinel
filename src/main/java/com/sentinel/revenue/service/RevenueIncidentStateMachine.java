package com.sentinel.revenue.service;

import com.sentinel.revenue.model.RevenueIncidentStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RevenueIncidentStateMachine {

    private static final Map<RevenueIncidentStatus, Set<RevenueIncidentStatus>> TRANSITIONS =
            buildTransitions();

    public RevenueIncidentStatus transition(RevenueIncidentStatus current,
                                            RevenueIncidentStatus requested) {
        Objects.requireNonNull(current, "current status is required");
        Objects.requireNonNull(requested, "requested status is required");

        if (!canTransition(current, requested)) {
            throw new IllegalStateException(
                    "Illegal revenue incident transition: " + current + " -> " + requested);
        }
        return requested;
    }

    public boolean canTransition(RevenueIncidentStatus current,
                                 RevenueIncidentStatus requested) {
        if (current == null || requested == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(current, Set.of()).contains(requested);
    }

    private static Map<RevenueIncidentStatus, Set<RevenueIncidentStatus>> buildTransitions() {
        EnumMap<RevenueIncidentStatus, Set<RevenueIncidentStatus>> transitions =
                new EnumMap<>(RevenueIncidentStatus.class);
        transitions.put(RevenueIncidentStatus.DETECTED,
                EnumSet.of(RevenueIncidentStatus.INVESTIGATING));
        transitions.put(RevenueIncidentStatus.INVESTIGATING,
                EnumSet.of(RevenueIncidentStatus.DIAGNOSED));
        transitions.put(RevenueIncidentStatus.DIAGNOSED,
                EnumSet.of(RevenueIncidentStatus.PLANNING));
        transitions.put(RevenueIncidentStatus.PLANNING,
                EnumSet.of(RevenueIncidentStatus.POLICY_REVIEW));
        transitions.put(RevenueIncidentStatus.POLICY_REVIEW,
                EnumSet.of(RevenueIncidentStatus.APPROVED, RevenueIncidentStatus.HUMAN_REVIEW));
        transitions.put(RevenueIncidentStatus.APPROVED,
                EnumSet.of(RevenueIncidentStatus.EXECUTING));
        transitions.put(RevenueIncidentStatus.HUMAN_REVIEW,
                EnumSet.of(RevenueIncidentStatus.EXECUTING));
        transitions.put(RevenueIncidentStatus.EXECUTING,
                EnumSet.of(RevenueIncidentStatus.MONITORING));
        transitions.put(RevenueIncidentStatus.MONITORING,
                EnumSet.of(RevenueIncidentStatus.RECOVERED,
                        RevenueIncidentStatus.FAILED,
                        RevenueIncidentStatus.STOPPED));
        return Map.copyOf(transitions);
    }
}
