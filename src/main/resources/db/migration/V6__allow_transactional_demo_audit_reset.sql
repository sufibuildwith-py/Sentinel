-- Audit history is immutable in ordinary application traffic. The demo reset endpoint
-- enables this transaction-local flag before deleting synthetic fixtures and their audit rows.
CREATE OR REPLACE FUNCTION prevent_audit_event_mutation()
RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' AND current_setting('sentinel.demo_reset', true) = 'true' THEN
        RETURN OLD;
    END IF;

    RAISE EXCEPTION 'audit_events is append-only';
END;
$$ LANGUAGE plpgsql;
