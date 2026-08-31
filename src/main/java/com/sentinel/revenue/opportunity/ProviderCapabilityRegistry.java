package com.sentinel.revenue.opportunity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class ProviderCapabilityRegistry {
    public static final String CATALOG_VERSION = "razorpay-action-marketplace-v1";

    private final List<RecoveryActionDefinition> catalog = List.of(
            RecoveryActionDefinition.noAction(),
            new RecoveryActionDefinition(OpportunityAction.WAIT_FOR_DOWNTIME_RECOVERY, CATALOG_VERSION,
                    ProviderCapability.PROVIDER_HEALTH_WAIT, Set.of("PROVIDER_DEGRADATION", "DOWNTIME"),
                    Set.of("UPI", "CARD", "NETBANKING"), BigDecimal.ZERO, null,
                    RecoveryRiskClass.LOW, "PaymentHealthAnalyzer", "PROVIDER_HEALTH_RECHECK",
                    "No provider mutation; re-evaluate after fresh provider-health evidence", true),
            new RecoveryActionDefinition(OpportunityAction.CREATE_PAYMENT_LINK, CATALOG_VERSION,
                    ProviderCapability.RAZORPAY_PAYMENT_LINKS, Set.of("RECOVERABLE_FAILURE"),
                    Set.of("UPI", "CARD", "NETBANKING"), new BigDecimal("0.6000"), null,
                    RecoveryRiskClass.LOW, "RazorpayGateway.createPaymentLink", "SIGNED_WEBHOOK_RECONCILIATION",
                    "Cancel an unpaid link; never reverse a confirmed payment", true),
            new RecoveryActionDefinition(OpportunityAction.CREATE_NEW_ORDER, CATALOG_VERSION,
                    ProviderCapability.RAZORPAY_ORDERS, Set.of("RECOVERABLE_FAILURE"),
                    Set.of("UPI", "CARD", "NETBANKING"), new BigDecimal("0.6500"), null,
                    RecoveryRiskClass.MEDIUM, "RazorpayAdapter.createOrder", "PAYMENT_RECONCILIATION",
                    "Do not create another order after provider-confirmed payment", true),
            new RecoveryActionDefinition(OpportunityAction.REQUEST_ALTERNATE_METHOD, CATALOG_VERSION,
                    ProviderCapability.RAZORPAY_PAYMENT_LINKS, Set.of("RAIL_DEGRADATION"),
                    Set.of("UPI", "CARD", "NETBANKING"), new BigDecimal("0.6000"), null,
                    RecoveryRiskClass.LOW, "RazorpayGateway.createPaymentLink", "SIGNED_WEBHOOK_RECONCILIATION",
                    "Offer only provider-supported methods and stop after confirmation", true),
            new RecoveryActionDefinition(OpportunityAction.CUSTOMER_OUTREACH, CATALOG_VERSION,
                    ProviderCapability.SIMULATED_CUSTOMER_COMMUNICATION, Set.of("RECOVERABLE_FAILURE"),
                    Set.of("UPI", "CARD", "NETBANKING"), new BigDecimal("0.6000"), null,
                    RecoveryRiskClass.LOW, "SimulationCommunicationAdapter", "SIMULATED_NOT_SENT",
                    "No real customer contact is made by the current adapter", false),
            new RecoveryActionDefinition(OpportunityAction.HUMAN_ESCALATION, CATALOG_VERSION,
                    ProviderCapability.HUMAN_WORKFLOW, Set.of("ANY"), Set.of("ANY"), BigDecimal.ZERO, null,
                    RecoveryRiskClass.MEDIUM, "Human approval queue", "PERSISTED_HUMAN_DECISION",
                    "A reviewer may reject or leave the case unexecuted", true));

    public Set<OpportunityAction> supportedActions() {
        EnumSet<OpportunityAction> actions = EnumSet.noneOf(OpportunityAction.class);
        catalog.forEach(entry -> actions.add(entry.action()));
        return Set.copyOf(actions);
    }

    public List<RecoveryActionDefinition> catalog() { return catalog; }

    public RecoveryActionDefinition requireDefinition(OpportunityAction action) {
        return catalog.stream().filter(entry -> entry.action() == action).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported recovery action: " + action));
    }
}
