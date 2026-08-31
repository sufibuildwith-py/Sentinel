ALTER TABLE provider_orders
    ADD COLUMN execution_mode VARCHAR(32) NOT NULL DEFAULT 'LEGACY_UNSPECIFIED';

ALTER TABLE recovery_actions
    ADD COLUMN execution_mode VARCHAR(32) NOT NULL DEFAULT 'LEGACY_UNSPECIFIED';

ALTER TABLE recovery_outcomes
    ADD COLUMN provider_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN confirmation_source VARCHAR(32);

UPDATE recovery_outcomes
SET provider_confirmed = TRUE,
    confirmation_source = 'LEGACY_RECONCILED'
WHERE source_event_id IS NOT NULL
  AND source_event_id NOT LIKE 'provider-order:%';

CREATE INDEX idx_recovery_outcomes_provider_confirmed
    ON recovery_outcomes (provider_confirmed, status);
