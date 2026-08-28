DROP INDEX IF EXISTS uk_recovery_actions_one_active_incident;

ALTER TABLE recovery_actions
    ADD COLUMN target_payment_id VARCHAR(128),
    ADD COLUMN target_customer_id VARCHAR(128),
    ADD COLUMN currency VARCHAR(3),
    ADD COLUMN provider_reference_id VARCHAR(40),
    ADD COLUMN external_resource_url TEXT,
    ADD COLUMN external_resource_status VARCHAR(32),
    ADD COLUMN execution_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN execution_claimed_at TIMESTAMPTZ,
    ADD COLUMN expires_at TIMESTAMPTZ,
    ADD COLUMN last_error_code VARCHAR(64),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uk_recovery_actions_provider_reference
    ON recovery_actions (provider_reference_id)
    WHERE provider_reference_id IS NOT NULL;

CREATE UNIQUE INDEX uk_recovery_actions_one_active_incident
    ON recovery_actions (incident_id)
    WHERE status IN ('PROPOSED', 'AUTO_APPROVED', 'PENDING_APPROVAL',
                     'APPROVED', 'EXECUTING', 'RETRY_PENDING',
                     'EXECUTION_UNCERTAIN');
