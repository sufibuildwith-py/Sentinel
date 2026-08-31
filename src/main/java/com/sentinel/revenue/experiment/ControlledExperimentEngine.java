package com.sentinel.revenue.experiment;

import com.sentinel.revenue.economics.EconomicEvidenceQuality;
import com.sentinel.revenue.opportunity.OpportunityAction;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ControlledExperimentEngine {
    public List<ExperimentAssignment> assign(ExperimentDefinition definition,
                                             List<ExperimentCandidate> candidates) {
        validate(definition);
        List<ExperimentAssignment> result = new ArrayList<>();
        long exposure = 0;
        for (ExperimentCandidate candidate : candidates.stream()
                .sorted(Comparator.comparing(item -> item.incidentId().toString())).toList()) {
            if (!candidate.policyEligible() || !candidate.governorEligible() || candidate.harmSignalPresent()) {
                result.add(blocked(definition, candidate, "SAFETY_INELIGIBLE")); continue;
            }
            ExperimentArm arm = select(definition, candidate.incidentId());
            long requested = arm.control() ? 0 : candidate.amountAtRiskMinor();
            if (exposure + requested > definition.maximumExposureMinor()) {
                result.add(blocked(definition, candidate, "EXPOSURE_BUDGET_EXHAUSTED")); continue;
            }
            exposure += requested;
            result.add(new ExperimentAssignment(definition.experimentId(), candidate.incidentId(), arm.name(),
                    arm.action(), arm.control(), "ASSIGNED", requested,
                    arm.control() ? "CONTROLLED_HOLDOUT_NO_EXECUTION" : "EXPERIMENT_PROPOSAL_ONLY"));
        }
        return List.copyOf(result);
    }

    public ExperimentSummary summarize(ExperimentDefinition definition,
                                       List<ExperimentObservation> observations) {
        validate(definition);
        Map<String, List<ExperimentObservation>> grouped = observations.stream()
                .collect(Collectors.groupingBy(ExperimentObservation::arm));
        List<ExperimentSummary.ArmResult> results = definition.arms().stream().map(arm -> {
            List<ExperimentObservation> sample = grouped.getOrDefault(arm.name(), List.of());
            int recovered = (int) sample.stream().filter(ExperimentObservation::providerConfirmedRecovery).count();
            long gross = sample.stream().filter(ExperimentObservation::providerConfirmedRecovery)
                    .mapToLong(ExperimentObservation::recoveredAmountMinor).sum();
            long cost = sample.stream().mapToLong(ExperimentObservation::recoveryCostMinor).sum();
            long harm = sample.stream().filter(ExperimentObservation::harmSignal).count();
            Long averageTtr = sample.stream().filter(item -> item.timeToRecovery() != null)
                    .mapToLong(item -> item.timeToRecovery().toMillis()).average().stream()
                    .mapToLong(Math::round).boxed().findFirst().orElse(null);
            return new ExperimentSummary.ArmResult(arm.name(), arm.control(), sample.size(), recovered,
                    gross, cost, gross - cost, sample.isEmpty() ? null : (double) recovered / sample.size(),
                    sample.isEmpty() ? null : (double) harm / sample.size(), averageTtr);
        }).toList();
        boolean minimum = results.stream().allMatch(result -> result.samples() >= definition.minimumSampleSizePerArm());
        boolean harm = results.stream().anyMatch(result -> result.harmRate() != null
                && result.harmRate() > definition.maximumHarmRate());
        EconomicEvidenceQuality quality = minimum && !harm
                ? EconomicEvidenceQuality.CONTROLLED_HOLDOUT : EconomicEvidenceQuality.EXPERIMENTAL;
        String conclusion = harm ? "STOP_HARM_THRESHOLD" : minimum ? "EVALUATION_READY" : "INSUFFICIENT_SAMPLE";
        return new ExperimentSummary(new ExperimentSummary.UUIDVersion(definition.experimentId().toString(),
                definition.policyVersion(), definition.modelVersion(), definition.seed()), results,
                minimum, harm, quality, conclusion, "NO_LIVE_AUTHORITY_CHANGE");
    }

    private ExperimentAssignment blocked(ExperimentDefinition definition, ExperimentCandidate candidate, String reason) {
        return new ExperimentAssignment(definition.experimentId(), candidate.incidentId(), "UNASSIGNED",
                OpportunityAction.NO_ACTION, true, reason, 0, "NO_AUTHORITY");
    }

    private ExperimentArm select(ExperimentDefinition definition, UUID incidentId) {
        int bucket = Math.floorMod(hash(definition.seed() + "|" + incidentId), 100);
        int cumulative = 0;
        for (ExperimentArm arm : definition.arms()) {
            cumulative += arm.allocationPercent();
            if (bucket < cumulative) return arm;
        }
        throw new IllegalStateException("Experiment allocation did not cover bucket");
    }

    private int hash(String input) {
        try { return ByteBuffer.wrap(MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8))).getInt(); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }

    private void validate(ExperimentDefinition definition) {
        if (definition == null || definition.experimentId() == null || definition.arms().isEmpty()
                || definition.maximumExposureMinor() < 0 || definition.minimumSampleSizePerArm() < 1
                || definition.maximumHarmRate() < 0 || definition.maximumHarmRate() > 1
                || definition.merchantApprovalReference() == null || definition.merchantApprovalReference().isBlank())
            throw new IllegalArgumentException("Experiment requires bounded limits and merchant approval");
        int total = definition.arms().stream().mapToInt(ExperimentArm::allocationPercent).sum();
        long controls = definition.arms().stream().filter(ExperimentArm::control).count();
        if (total != 100 || controls != 1 || definition.arms().stream().anyMatch(arm -> arm.allocationPercent() <= 0))
            throw new IllegalArgumentException("Experiment requires exactly one control and 100% allocation");
        ExperimentArm control = definition.arms().stream().filter(ExperimentArm::control).findFirst().orElseThrow();
        if (control.action() != OpportunityAction.NO_ACTION)
            throw new IllegalArgumentException("Control arm must be NO_ACTION");
    }
}
