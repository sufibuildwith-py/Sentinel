CREATE TABLE policy_replay_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), incident_id UUID NOT NULL REFERENCES revenue_incidents(incident_id),
    snapshot_version VARCHAR(32) NOT NULL, policy_context JSONB NOT NULL, governor_context JSONB NOT NULL,
    feature_schema_version VARCHAR(32) NOT NULL, model_version VARCHAR(64) NOT NULL,
    policy_version VARCHAR(64) NOT NULL, strategy_version VARCHAR(64) NOT NULL,
    governor_version VARCHAR(64) NOT NULL, replay_seed BIGINT NOT NULL,
    production_policy_result VARCHAR(16) NOT NULL, production_action VARCHAR(64) NOT NULL,
    production_governor_result VARCHAR(16) NOT NULL,
    production_predicted_value_minor BIGINT, production_confidence NUMERIC(5,4),
    snapshot_sha256 VARCHAR(64) NOT NULL UNIQUE,
    captured_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE shadow_decision_differences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), snapshot_id UUID NOT NULL REFERENCES policy_replay_snapshots(id),
    production_action VARCHAR(64) NOT NULL, shadow_action VARCHAR(64) NOT NULL,
    production_policy_result VARCHAR(16) NOT NULL, shadow_policy_result VARCHAR(16) NOT NULL,
    production_governor_result VARCHAR(16) NOT NULL, shadow_governor_result VARCHAR(16) NOT NULL,
    production_predicted_value_minor BIGINT, shadow_predicted_value_minor BIGINT,
    production_confidence NUMERIC(5,4), shadow_confidence NUMERIC(5,4),
    production_priority NUMERIC(8,4), shadow_priority NUMERIC(8,4),
    opportunity_ranking_changed BOOLEAN NOT NULL, approval_requirement_changed BOOLEAN NOT NULL,
    explanation TEXT NOT NULL, critical_regression BOOLEAN NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_policy_replay_incident_captured
    ON policy_replay_snapshots(incident_id, captured_at DESC);
CREATE INDEX idx_shadow_difference_snapshot_created
    ON shadow_decision_differences(snapshot_id, created_at DESC);
CREATE INDEX idx_shadow_critical_regression
    ON shadow_decision_differences(critical_regression)
    WHERE critical_regression = TRUE;
