package com.sentinel.revenue.model;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "customer_contact_preferences")
public class CustomerContactPreference {
    @Id @Column(name = "customer_ref", length = 128) private String customerRef;
    @Column(name = "consent_granted", nullable = false) private boolean consentGranted;
    @Column(name = "do_not_contact", nullable = false) private boolean doNotContact;
    @Column(name = "opted_out", nullable = false) private boolean optedOut;
    @Column(nullable = false, length = 64) private String timezone;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected CustomerContactPreference() { }
    public CustomerContactPreference(String customerRef, boolean consentGranted, boolean doNotContact,
                                     boolean optedOut, String timezone, Instant updatedAt) {
        this.customerRef = customerRef; this.consentGranted = consentGranted; this.doNotContact = doNotContact;
        this.optedOut = optedOut; this.timezone = timezone; this.updatedAt = updatedAt;
    }
    public String getCustomerRef() { return customerRef; }
    public boolean isConsentGranted() { return consentGranted; }
    public boolean isDoNotContact() { return doNotContact; }
    public boolean isOptedOut() { return optedOut; }
    public String getTimezone() { return timezone; }
}
