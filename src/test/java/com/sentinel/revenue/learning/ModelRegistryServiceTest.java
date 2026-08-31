package com.sentinel.revenue.learning;

import com.sentinel.evaluation.*;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ModelRegistryServiceTest {
    @Test
    void humanApprovalAndPassingExistingSafetyGatesAreRequiredForPromotion() {
        RegisteredModelRepository models = mock(RegisteredModelRepository.class);
        ModelPromotionApprovalRepository approvals = mock(ModelPromotionApprovalRepository.class);
        EvaluationReportService evaluation = mock(EvaluationReportService.class);
        RegisteredModel model = new RegisteredModel("opportunity-ranker", "v1", "opportunity-v1", Instant.now());
        UUID id = UUID.randomUUID(); ReflectionTestUtils.setField(model, "id", id);
        when(models.findById(id)).thenReturn(Optional.of(model));
        when(models.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        EvaluationReport report = mock(EvaluationReport.class);
        when(report.duplicateFinancialEffects()).thenReturn(0);
        when(report.safetyGates()).thenReturn(List.of(new EvaluationReport.SafetyGate("unsafe",0,"==0",true,"passed")));
        when(report.policyCompliance()).thenReturn(new EvaluationReport.Score(432,432,1));
        when(report.reportVersion()).thenReturn("phase9-v1"); when(report.seed()).thenReturn(20260901L);
        when(evaluation.report()).thenReturn(report);
        ModelRegistryService registry = new ModelRegistryService(models, approvals, evaluation);

        assertThatThrownBy(() -> registry.promote(id, ModelLifecycle.SHADOW, "SENTINEL", "self promote"))
                .isInstanceOf(IllegalArgumentException.class);
        RegisteredModel promoted = registry.promote(id, ModelLifecycle.SHADOW, "reviewer-42", "Replay gates passed");
        assertThat(promoted.getLifecycle()).isEqualTo(ModelLifecycle.SHADOW);
        verify(approvals).saveAndFlush(any(ModelPromotionApproval.class));
    }
}
