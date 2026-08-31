package com.sentinel.revenue.model;
import com.sentinel.revenue.communication.CustomerIntent;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.*;

@Entity @Table(name = "customer_interactions")
public class CustomerInteraction {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "incident_id", nullable = false) private UUID incidentId;
    @Column(name = "recovery_action_id") private UUID recoveryActionId;
    @Column(name = "customer_ref", nullable = false, length = 128) private String customerRef;
    @Column(nullable = false, length = 32) private String channel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private CustomerIntent intent;
    @Column(name = "template_id", nullable = false, length = 64) private String templateId;
    @Column(name = "delivery_mode", nullable = false, length = 32) private String deliveryMode;
    @Column(nullable = false, length = 32) private String status;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "policy_trace", nullable = false, columnDefinition = "jsonb")
    private List<String> policyTrace = new ArrayList<>();
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected CustomerInteraction() { }
    public CustomerInteraction(UUID incidentId, UUID actionId, String customerRef, String channel,
                               CustomerIntent intent, String templateId, String mode, String status,
                               List<String> trace, Instant now) {
        this.incidentId = incidentId; this.recoveryActionId = actionId; this.customerRef = customerRef;
        this.channel = channel; this.intent = intent; this.templateId = templateId;
        this.deliveryMode = mode; this.status = status; this.policyTrace = new ArrayList<>(trace); this.createdAt = now;
    }
    public UUID getId() { return id; } public UUID getIncidentId() { return incidentId; }
    public UUID getRecoveryActionId() { return recoveryActionId; } public String getCustomerRef() { return customerRef; }
    public String getChannel() { return channel; }
    public String getDeliveryMode() { return deliveryMode; } public String getStatus() { return status; }
    public List<String> getPolicyTrace() { return List.copyOf(policyTrace); }
    public Instant getCreatedAt() { return createdAt; }
}
