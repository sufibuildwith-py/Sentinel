package com.sentinel.revenue.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "registered_models")
public class RegisteredModel {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "model_name", nullable = false, length = 128) private String modelName;
    @Column(name = "model_version", nullable = false, length = 64) private String modelVersion;
    @Column(name = "feature_schema_version", nullable = false, length = 32) private String featureSchemaVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private ModelLifecycle lifecycle;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected RegisteredModel() { }
    public RegisteredModel(String name, String version, String schema, Instant now) {
        this.modelName = name; this.modelVersion = version; this.featureSchemaVersion = schema;
        this.lifecycle = ModelLifecycle.CANDIDATE; this.createdAt = now; this.updatedAt = now;
    }
    public void promote(ModelLifecycle target, Instant now) {
        boolean valid = target == ModelLifecycle.RETIRED || switch (lifecycle) {
            case CANDIDATE -> target == ModelLifecycle.SHADOW;
            case SHADOW -> target == ModelLifecycle.CHALLENGER;
            case CHALLENGER -> target == ModelLifecycle.CHAMPION;
            case CHAMPION, RETIRED -> false;
        };
        if (!valid) throw new IllegalStateException("Illegal model lifecycle transition " + lifecycle + " → " + target);
        lifecycle = target; updatedAt = now;
    }
    public UUID getId() { return id; } public String getModelName() { return modelName; }
    public String getModelVersion() { return modelVersion; } public String getFeatureSchemaVersion() { return featureSchemaVersion; }
    public ModelLifecycle getLifecycle() { return lifecycle; } public Instant getCreatedAt() { return createdAt; }
}
