CREATE UNIQUE INDEX uk_recovery_actions_one_active_incident
    ON recovery_actions (incident_id)
    WHERE status IN ('PROPOSED', 'AUTO_APPROVED', 'PENDING_APPROVAL',
                     'APPROVED', 'EXECUTING');

CREATE OR REPLACE FUNCTION prevent_audit_event_mutation()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_events is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_events_append_only
    BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_event_mutation();
