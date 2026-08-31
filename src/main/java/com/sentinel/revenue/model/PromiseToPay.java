package com.sentinel.revenue.model;
import com.sentinel.revenue.communication.PromiseStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.*;

@Entity @Table(name = "promises_to_pay")
public class PromiseToPay {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "incident_id", nullable = false) private UUID incidentId;
    @Column(name = "recovery_action_id") private UUID recoveryActionId;
    @Column(name = "customer_ref", nullable = false, length = 128) private String customerRef;
    @Column(name = "promised_amount_minor", nullable = false) private long promisedAmountMinor;
    @Column(name = "balance_minor", nullable = false) private long balanceMinor;
    @Column(name = "due_at", nullable = false) private Instant dueAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private PromiseStatus status;
    @Column(name = "fulfilled_amount_minor", nullable = false) private long fulfilledAmountMinor;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "source_event_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> sourceEventIds = new ArrayList<>();
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected PromiseToPay() { }
    public PromiseToPay(UUID incidentId, UUID actionId, String customerRef, long amount,
                        long balance, Instant dueAt, Instant now) {
        this.incidentId = incidentId; this.recoveryActionId = actionId; this.customerRef = customerRef;
        this.promisedAmountMinor = amount; this.balanceMinor = balance; this.dueAt = dueAt;
        this.status = PromiseStatus.PENDING; this.createdAt = now; this.updatedAt = now;
    }
    public boolean applyConfirmedPayment(long amount, String eventId, Instant now) {
        if (sourceEventIds.contains(eventId) || status == PromiseStatus.KEPT || status == PromiseStatus.CANCELLED) return false;
        sourceEventIds.add(eventId); fulfilledAmountMinor = Math.min(promisedAmountMinor, fulfilledAmountMinor + amount);
        status = fulfilledAmountMinor >= promisedAmountMinor ? PromiseStatus.KEPT : PromiseStatus.PARTIALLY_KEPT;
        updatedAt = now; return true;
    }
    public UUID getId() { return id; } public UUID getIncidentId() { return incidentId; }
    public UUID getRecoveryActionId() { return recoveryActionId; } public String getCustomerRef() { return customerRef; }
    public long getPromisedAmountMinor() { return promisedAmountMinor; } public long getBalanceMinor() { return balanceMinor; }
    public Instant getDueAt() { return dueAt; } public PromiseStatus getStatus() { return status; }
    public long getFulfilledAmountMinor() { return fulfilledAmountMinor; }
    public List<String> getSourceEventIds() { return List.copyOf(sourceEventIds); }
}
