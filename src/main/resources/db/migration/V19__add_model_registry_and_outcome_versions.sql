ALTER TABLE recovery_outcomes
    ADD COLUMN feature_schema_version VARCHAR(32) NOT NULL DEFAULT 'recovery-v1',
    ADD COLUMN model_version VARCHAR(64) NOT NULL DEFAULT 'none-deterministic',
    ADD COLUMN policy_version VARCHAR(64) NOT NULL DEFAULT 'policy-v1',
    ADD COLUMN strategy_version VARCHAR(64) NOT NULL DEFAULT 'strategy-v1';

CREATE TABLE registered_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_name VARCHAR(128) NOT NULL,
    model_version VARCHAR(64) NOT NULL,
    feature_schema_version VARCHAR(32) NOT NULL,
    lifecycle VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_registered_model_version UNIQUE(model_name, model_version)
);

CREATE TABLE model_promotion_approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_id UUID NOT NULL REFERENCES registered_models(id),
    from_lifecycle VARCHAR(16) NOT NULL,
    to_lifecycle VARCHAR(16) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    reason TEXT NOT NULL,
    evaluation_report_version VARCHAR(64) NOT NULL,
    evaluation_seed BIGINT NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE policy_change_proposals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proposed_by_model_id UUID REFERENCES registered_models(id),
    policy_version VARCHAR(64) NOT NULL,
    proposal TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PROPOSED',
    replay_passed BOOLEAN NOT NULL DEFAULT FALSE,
    shadow_passed BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by VARCHAR(128),
    approval_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    approved_at TIMESTAMPTZ
);
