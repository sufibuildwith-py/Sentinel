ALTER TABLE processed_webhooks
    ADD COLUMN provider_link_id VARCHAR(128),
    ADD COLUMN payload_digest VARCHAR(64),
    ADD COLUMN disposition VARCHAR(32) NOT NULL DEFAULT 'RECEIVED',
    ADD COLUMN processing_error VARCHAR(64);

CREATE INDEX idx_processed_webhooks_link ON processed_webhooks (provider_link_id);

CREATE TABLE webhook_security_events (
    id UUID PRIMARY KEY,
    received_at TIMESTAMPTZ NOT NULL,
    request_digest VARCHAR(64) NOT NULL,
    signature_header_present BOOLEAN NOT NULL,
    event_id_header_present BOOLEAN NOT NULL,
    reason VARCHAR(64) NOT NULL
);

CREATE OR REPLACE FUNCTION prevent_webhook_security_event_mutation()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'webhook_security_events is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_webhook_security_events_append_only
    BEFORE UPDATE OR DELETE ON webhook_security_events
    FOR EACH ROW EXECUTE FUNCTION prevent_webhook_security_event_mutation();

CREATE UNIQUE INDEX uk_historical_incident_original
    ON historical_incidents (original_incident_id)
    WHERE original_incident_id IS NOT NULL;
