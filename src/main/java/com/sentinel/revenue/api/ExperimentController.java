package com.sentinel.revenue.api;

import com.sentinel.revenue.experiment.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/revenue/experiments")
public class ExperimentController {
    private final ControlledExperimentEngine experiments;
    private final RegretAnalysisService regret;

    public ExperimentController(ControlledExperimentEngine experiments, RegretAnalysisService regret) {
        this.experiments = experiments; this.regret = regret;
    }

    @PostMapping("/assign")
    public ResponseEntity<List<ExperimentAssignment>> assign(@RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(experiments.assign(request.definition(), request.candidates()));
    }

    @PostMapping("/summarize")
    public ResponseEntity<ExperimentSummary> summarize(@RequestBody SummaryRequest request) {
        return ResponseEntity.ok(experiments.summarize(request.definition(), request.observations()));
    }

    @PostMapping("/regret")
    public ResponseEntity<RegretAnalysis> regret(@RequestBody RegretRequest request) {
        return ResponseEntity.ok(regret.analyze(request.incidentId(), request.actualAction(),
                request.actualNetIncrementalValueMinor(), request.alternatives(),
                request.policyVersion(), request.modelVersion()));
    }

    public record AssignmentRequest(ExperimentDefinition definition, List<ExperimentCandidate> candidates) {
        public AssignmentRequest { candidates = List.copyOf(candidates); }
    }
    public record SummaryRequest(ExperimentDefinition definition, List<ExperimentObservation> observations) {
        public SummaryRequest { observations = List.copyOf(observations); }
    }
    public record RegretRequest(UUID incidentId, com.sentinel.revenue.opportunity.OpportunityAction actualAction,
                                BigDecimal actualNetIncrementalValueMinor, List<RegretCandidate> alternatives,
                                String policyVersion, String modelVersion) {
        public RegretRequest { alternatives = List.copyOf(alternatives); }
    }
}
