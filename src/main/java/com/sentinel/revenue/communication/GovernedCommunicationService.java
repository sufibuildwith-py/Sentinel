package com.sentinel.revenue.communication;

import com.sentinel.revenue.governor.*;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class GovernedCommunicationService {
    private final CommunicationAdapter adapter;
    private final CommunicationProperties properties;
    private final CustomerContactPreferenceRepository preferences;
    private final CustomerInteractionRepository interactions;
    private final KillSwitchService killSwitches;
    public GovernedCommunicationService(CommunicationAdapter adapter, CommunicationProperties properties,
                                        CustomerContactPreferenceRepository preferences,
                                        CustomerInteractionRepository interactions, KillSwitchService killSwitches) {
        this.adapter = adapter; this.properties = properties; this.preferences = preferences;
        this.interactions = interactions; this.killSwitches = killSwitches;
    }
    @Transactional
    public CustomerInteraction communicate(UUID incidentId, UUID actionId, String customerRef,
                                           String channel, CustomerIntent intent, String templateId,
                                           Instant now) {
        Instant at = now == null ? Instant.now() : now;
        List<String> trace = validate(customerRef, templateId, at);
        CommunicationResult result = adapter.send(customerRef, channel, templateId);
        return interactions.saveAndFlush(new CustomerInteraction(incidentId, actionId, customerRef,
                channel, intent == null ? CustomerIntent.UNKNOWN : intent, templateId,
                result.mode(), result.status(), trace, at));
    }
    private List<String> validate(String customerRef, String templateId, Instant now) {
        List<String> trace = new ArrayList<>();
        if (killSwitches.enabled(KillSwitch.CUSTOMER_OUTREACH)) deny("CUSTOMER_OUTREACH_KILL_SWITCH");
        CustomerContactPreference preference = preferences.findById(customerRef)
                .orElseThrow(() -> new IllegalStateException("CONTACT_PREFERENCE_REQUIRED"));
        if (!preference.isConsentGranted()) deny("CONSENT_REQUIRED");
        if (preference.isDoNotContact()) deny("DO_NOT_CONTACT");
        if (preference.isOptedOut()) deny("OPTED_OUT");
        if (!properties.allowedTemplates().contains(templateId)) deny("UNAPPROVED_MESSAGE_TEMPLATE");
        ZoneId zone;
        try { zone = ZoneId.of(preference.getTimezone()); } catch (Exception invalid) { zone = ZoneOffset.UTC; }
        LocalTime local = now.atZone(zone).toLocalTime();
        if (inQuietHours(local)) deny("QUIET_HOURS");
        long recent = interactions.countByCustomerRefAndCreatedAtAfter(customerRef, now.minus(Duration.ofHours(24)));
        if (recent >= properties.maxContactsPer24Hours()) deny("CONTACT_FREQUENCY_LIMIT");
        trace.addAll(List.of("CONSENT PASS", "DNC PASS", "OPT_OUT PASS", "QUIET_HOURS PASS",
                "CONTACT_FREQUENCY PASS actual=" + recent, "TEMPLATE_ALLOWLIST PASS", "MODE=" + adapter.mode()));
        return trace;
    }
    private boolean inQuietHours(LocalTime time) {
        LocalTime start = properties.quietHoursStart(), end = properties.quietHoursEnd();
        return start.isBefore(end) ? !time.isBefore(start) && time.isBefore(end)
                : !time.isBefore(start) || time.isBefore(end);
    }
    private void deny(String reason) { throw new IllegalStateException(reason); }
}
