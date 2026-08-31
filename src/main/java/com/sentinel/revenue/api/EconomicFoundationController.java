package com.sentinel.revenue.api;

import com.sentinel.revenue.economics.DecisionCertificateService;
import com.sentinel.revenue.economics.CounterfactualEstimate;
import com.sentinel.revenue.economics.CounterfactualRecoveryEngine;
import com.sentinel.revenue.economics.TimingChannelOptimizer;
import com.sentinel.revenue.economics.TimingChannelRecommendation;
import com.sentinel.revenue.economics.RecoveryCostLedgerService;
import com.sentinel.revenue.model.DecisionCertificate;
import com.sentinel.revenue.model.RecoveryCostEntry;
import com.sentinel.revenue.opportunity.ProviderCapabilityRegistry;
import com.sentinel.revenue.opportunity.RecoveryActionDefinition;
import com.sentinel.revenue.opportunity.OpportunityProperties;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/revenue")
public class EconomicFoundationController {
    private final ProviderCapabilityRegistry marketplace;
    private final RecoveryCostLedgerService costs;
    private final DecisionCertificateService certificates;
    private final CounterfactualRecoveryEngine counterfactuals;
    private final RevenueIncidentRepository incidents;
    private final TimingChannelOptimizer timing;
    private final OpportunityProperties opportunityProperties;

    public EconomicFoundationController(ProviderCapabilityRegistry marketplace,
                                        RecoveryCostLedgerService costs,
                                        DecisionCertificateService certificates,
                                        CounterfactualRecoveryEngine counterfactuals,
                                        RevenueIncidentRepository incidents,
                                        TimingChannelOptimizer timing,
                                        OpportunityProperties opportunityProperties) {
        this.marketplace = marketplace; this.costs = costs; this.certificates = certificates;
        this.counterfactuals = counterfactuals; this.incidents = incidents;
        this.timing = timing; this.opportunityProperties = opportunityProperties;
    }

    @GetMapping("/action-marketplace")
    public ResponseEntity<ActionMarketplaceView> actionMarketplace() {
        return ResponseEntity.ok(new ActionMarketplaceView(ProviderCapabilityRegistry.CATALOG_VERSION,
                marketplace.catalog()));
    }

    @GetMapping("/incidents/{incidentId}/recovery-costs")
    public ResponseEntity<List<RecoveryCostEntry>> recoveryCosts(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(costs.forIncident(incidentId));
    }

    @GetMapping("/incidents/{incidentId}/decision-certificates")
    public ResponseEntity<List<DecisionCertificate>> decisionCertificates(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(certificates.forIncident(incidentId));
    }

    @GetMapping("/incidents/{incidentId}/counterfactuals")
    public ResponseEntity<List<CounterfactualEstimate>> counterfactuals(@PathVariable UUID incidentId) {
        var incident = incidents.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Revenue incident not found: " + incidentId));
        List<CounterfactualEstimate> estimates = marketplace.supportedActions().stream()
                .sorted(java.util.Comparator.comparing(Enum::name))
                .map(action -> counterfactuals.estimate(incident, action, opportunityProperties.maturity())).toList();
        return ResponseEntity.ok(estimates);
    }

    @GetMapping("/incidents/{incidentId}/timing-recommendation")
    public ResponseEntity<TimingChannelRecommendation> timingRecommendation(@PathVariable UUID incidentId) {
        var incident = incidents.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Revenue incident not found: " + incidentId));
        return ResponseEntity.ok(timing.recommend(incident, "ALL", opportunityProperties.maturity()));
    }

    public record ActionMarketplaceView(String version, List<RecoveryActionDefinition> actions) {
        public ActionMarketplaceView { actions = List.copyOf(actions); }
    }
}
