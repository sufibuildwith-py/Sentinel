package com.sentinel.revenue.learning;

import com.sentinel.evaluation.*;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class ModelRegistryService {
    private final RegisteredModelRepository models;
    private final ModelPromotionApprovalRepository approvals;
    private final EvaluationReportService evaluation;
    private final ShadowDecisionDifferenceRepository shadowDifferences;
    @Autowired
    public ModelRegistryService(RegisteredModelRepository models, ModelPromotionApprovalRepository approvals,
                                EvaluationReportService evaluation,
                                ShadowDecisionDifferenceRepository shadowDifferences) {
        this.models=models; this.approvals=approvals; this.evaluation=evaluation;
        this.shadowDifferences=shadowDifferences;
    }
    public ModelRegistryService(RegisteredModelRepository models, ModelPromotionApprovalRepository approvals,
                                EvaluationReportService evaluation) {
        this(models, approvals, evaluation, null);
    }
    @Transactional
    public RegisteredModel register(String name, String version, String featureSchema) {
        if (models.findByModelNameAndModelVersion(name, version).isPresent())
            throw new IllegalStateException("Model version already registered");
        return models.saveAndFlush(new RegisteredModel(name, version, featureSchema, Instant.now()));
    }
    @Transactional
    public RegisteredModel promote(UUID modelId, ModelLifecycle target, String actor, String reason) {
        RegisteredModel model = models.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));
        if (actor == null || actor.isBlank() || reason == null || reason.isBlank()
                || actor.equalsIgnoreCase("SENTINEL") || actor.equalsIgnoreCase(model.getModelName()))
            throw new IllegalArgumentException("Persisted human actor and reason are required");
        EvaluationReport report = evaluation.report();
        boolean unsafe = report.duplicateFinancialEffects() != 0
                || report.safetyGates().stream().anyMatch(gate -> !gate.passed())
                || report.policyCompliance().numerator() != report.policyCompliance().denominator();
        if (unsafe) throw new IllegalStateException("Critical evaluation regression blocks promotion");
        if (shadowDifferences != null && shadowDifferences.existsByCriticalRegressionTrue())
            throw new IllegalStateException("Critical replay/shadow regression blocks promotion");
        ModelLifecycle from = model.getLifecycle();
        model.promote(target, Instant.now());
        approvals.saveAndFlush(new ModelPromotionApproval(modelId, from, target, actor, reason,
                report.reportVersion(), report.seed(), Instant.now()));
        return models.saveAndFlush(model);
    }
    @Transactional(readOnly = true) public List<RegisteredModel> all() { return models.findAll(); }
}
