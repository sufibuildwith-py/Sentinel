package com.sentinel.evaluation;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("evaluationDataset")
public class EvaluationReadinessHealthIndicator implements HealthIndicator {
    private final EvaluationDatasetGenerator dataset;

    public EvaluationReadinessHealthIndicator(EvaluationDatasetGenerator dataset) { this.dataset = dataset; }

    @Override
    public Health health() {
        int size = dataset.generate().size();
        return size >= 300 && size <= 500
                ? Health.up().withDetail("scenarioCount", size).build()
                : Health.down().withDetail("reason", "scenario count outside 300-500 safety bound").build();
    }
}
