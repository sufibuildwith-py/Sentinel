package com.sentinel.revenue.opportunity;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class ProviderCapabilityRegistry {
    public Set<OpportunityAction> supportedActions() {
        return EnumSet.of(OpportunityAction.NO_ACTION, OpportunityAction.WAIT_FOR_DOWNTIME_RECOVERY,
                OpportunityAction.CREATE_PAYMENT_LINK, OpportunityAction.CREATE_NEW_ORDER,
                OpportunityAction.REQUEST_ALTERNATE_METHOD, OpportunityAction.CUSTOMER_OUTREACH,
                OpportunityAction.HUMAN_ESCALATION);
    }
}
