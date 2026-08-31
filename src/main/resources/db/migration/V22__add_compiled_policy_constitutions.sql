CREATE TABLE compiled_policy_constitutions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL,
    merchant_id VARCHAR(128) NOT NULL,
    policy_version VARCHAR(64) NOT NULL,
    compiler_version VARCHAR(64) NOT NULL,
    constitution JSONB NOT NULL,
    constitution_sha256 VARCHAR(64) NOT NULL,
    effective_at TIMESTAMPTZ,
    approval_reference VARCHAR(128),
    benchmark_reference VARCHAR(128),
    replay_reference VARCHAR(128),
    shadow_reference VARCHAR(128),
    status VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_compiled_policy_merchant_version UNIQUE (merchant_id, policy_version),
    CONSTRAINT uk_compiled_policy_hash UNIQUE (merchant_id, constitution_sha256)
);

CREATE INDEX idx_compiled_policy_merchant_created
    ON compiled_policy_constitutions(merchant_id, created_at DESC);
