import { describe, expect, it } from "vitest";
import { demoIncidentId, fixtureAuditByIncidentId, fixtureIncidentDetails, fixtureIncidents } from "./fixtures";

describe("incident-scoped fixture contract", () => {
  it("keeps provider truth, evidence, actions, and audit events owned by their incident", () => {
    const recovered = fixtureIncidentDetails[demoIncidentId];
    const pendingId = fixtureIncidents[1].incidentId;
    const pending = fixtureIncidentDetails[pendingId];
    const recoveredAudit = fixtureAuditByIncidentId[demoIncidentId];
    const pendingAudit = fixtureAuditByIncidentId[pendingId];

    expect(recovered.truth).toMatchObject({ providerAccepted: true, providerConfirmed: true });
    expect(recovered.action?.providerId).toBe("plink_test_sentinel");
    expect(pending.truth).toMatchObject({ providerAccepted: false, providerConfirmed: false });
    expect(pending.action?.providerId).toBeUndefined();
    expect(pending.findings).toEqual([]);
    expect(pending.action?.actionId).not.toBe(recovered.action?.actionId);
    expect(pendingAudit.map((entry) => entry.eventId)).not.toEqual(expect.arrayContaining(recoveredAudit.map((entry) => entry.eventId)));
    expect(JSON.stringify(pending)).not.toContain("plink_test_sentinel");
    expect(JSON.stringify(pendingAudit)).not.toContain("payment_link.paid");
  });

  it("keeps policy-denied and historical universes free of operational provider truth", () => {
    const denied = fixtureIncidentDetails[fixtureIncidents[2].incidentId];
    expect(denied.action).toMatchObject({ status: "STOPPED", policyDecision: "DENY" });
    expect(denied.action?.providerId).toBeUndefined();
    expect(denied.truth).toBeNull();
  });
});
