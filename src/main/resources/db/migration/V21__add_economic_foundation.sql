CREATE TABLE recovery_cost_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES revenue_incidents(incident_id),
    recovery_action_id UUID REFERENCES recovery_actions(id),
    decision_id UUID,
    cost_category VARCHAR(64) NOT NULL,
    amount_minor NUMERIC(19,0) NOT NULL CHECK (amount_minor >= 0),
    currency VARCHAR(3) NOT NULL,
    source VARCHAR(128) NOT NULL,
    calculation_method VARCHAR(128) NOT NULL,
    evidence_quality VARCHAR(32) NOT NULL,
    cost_version VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recovery_cost_incident_time
    ON recovery_cost_entries(incident_id, occurred_at);
CREATE INDEX idx_recovery_cost_action
    ON recovery_cost_entries(recovery_action_id)
    WHERE recovery_action_id IS NOT NULL;

CREATE TABLE decision_certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    decision_id UUID NOT NULL UNIQUE,
    incident_id UUID NOT NULL REFERENCES revenue_incidents(incident_id),
    recovery_action_id UUID REFERENCES recovery_actions(id),
    decision_type VARCHAR(64) NOT NULL,
    policy_version VARCHAR(64) NOT NULL,
    model_version VARCHAR(64) NOT NULL,
    feature_schema_version VARCHAR(64) NOT NULL,
    strategy_version VARCHAR(64) NOT NULL,
    input_snapshot_hash VARCHAR(64) NOT NULL,
    evidence_capsule_hash VARCHAR(64),
    candidate_actions JSONB NOT NULL DEFAULT '[]'::jsonb,
    rejected_alternatives JSONB NOT NULL DEFAULT '[]'::jsonb,
    selected_action VARCHAR(64) NOT NULL,
    counterfactual_method VARCHAR(64) NOT NULL,
    evidence_quality VARCHAR(32) NOT NULL,
    expected_incremental_value_minor NUMERIC(19,0),
    authorization_result VARCHAR(64) NOT NULL,
    exposure_decision VARCHAR(64) NOT NULL,
    execution_reference VARCHAR(128),
    provider_reference VARCHAR(128),
    reconciliation_reference VARCHAR(128),
    attribution_reference VARCHAR(128),
    final_truth_state VARCHAR(64) NOT NULL,
    certificate_version VARCHAR(32) NOT NULL,
    certificate_sha256 VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_decision_certificate_incident_time
    ON decision_certificates(incident_id, created_at);
CREATE INDEX idx_decision_certificate_action
    ON decision_certificates(recovery_action_id)
    WHERE recovery_action_id IS NOT NULL;
