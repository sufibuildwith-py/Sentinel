package com.sentinel.revenue.health;

import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class SystemicIncidentService {
    private final SystemicRecoveryIncidentRepository systemic;
    private final SystemicIncidentMemberRepository members;
    private final RevenueIncidentRepository incidents;

    public SystemicIncidentService(SystemicRecoveryIncidentRepository systemic,
                                   SystemicIncidentMemberRepository members,
                                   RevenueIncidentRepository incidents) {
        this.systemic = systemic; this.members = members; this.incidents = incidents;
    }

    @Transactional
    public SystemicRecoveryIncident correlate(String merchantId, PaymentHealthReport health,
                                               List<UUID> incidentIds, Instant now) {
        List<PaymentHealthSignal> active = health.signals().stream()
                .filter(PaymentHealthSignal::active).toList();
        if (active.isEmpty()) throw new IllegalStateException("No active systemic signal to correlate");
        List<RevenueIncident> children = incidents.findAllById(incidentIds).stream()
                .filter(incident -> !members.existsByPaymentIncidentIncidentId(incident.getIncidentId()))
                .toList();
        if (children.size() < 2) throw new IllegalArgumentException(
                "A systemic incident requires at least two uncorrelated payment incidents");
        List<RootCauseCandidate> candidates = active.stream().map(signal -> new RootCauseCandidate(
                        signal.type().name(), confidence(signal), signal.evidence(),
                        List.of("baseline=" + signal.baseline()), signal.scope()))
                .sorted(Comparator.comparingDouble(RootCauseCandidate::confidence).reversed()).toList();
        String scope = active.stream().map(PaymentHealthSignal::scope).distinct()
                .sorted().reduce((left, right) -> left + "," + right).orElse("merchant");
        Instant at = now == null ? Instant.now() : now;
        SystemicRecoveryIncident parent = systemic.saveAndFlush(
                new SystemicRecoveryIncident(merchantId, scope, candidates, at));
        members.saveAll(children.stream().map(child -> new SystemicIncidentMember(parent, child, at)).toList());
        return parent;
    }

    private double confidence(PaymentHealthSignal signal) {
        if (signal.threshold() <= 0) return signal.active() ? 0.75 : 0;
        return Math.min(0.99, Math.max(0, signal.actual() / signal.threshold()) * 0.75);
    }
}
