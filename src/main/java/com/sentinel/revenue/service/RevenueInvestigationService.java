package com.sentinel.revenue.service;

import com.sentinel.core.agent.AgentContext;
import com.sentinel.core.agent.AgentResult;
import com.sentinel.revenue.investigation.*;
import com.sentinel.revenue.model.FindingSource;
import com.sentinel.revenue.model.IncidentFinding;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.repository.IncidentFindingRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RevenueInvestigationService {
    private final RevenueIncidentRepository incidents;
    private final IncidentFindingRepository findings;
    private final TriageAgent triageAgent;
    private final PaymentAnalystAgent analystAgent;
    private final HistoricalMemoryService memory;
    private final RootCauseAgent rootCauseAgent;
    private final RevenueIncidentStateMachine stateMachine = new RevenueIncidentStateMachine();

    public RevenueInvestigationService(RevenueIncidentRepository incidents,
                                       IncidentFindingRepository findings,
                                       TriageAgent triageAgent,
                                       PaymentAnalystAgent analystAgent,
                                       HistoricalMemoryService memory,
                                       RootCauseAgent rootCauseAgent) {
        this.incidents = incidents;
        this.findings = findings;
        this.triageAgent = triageAgent;
        this.analystAgent = analystAgent;
        this.memory = memory;
        this.rootCauseAgent = rootCauseAgent;
    }

    @Transactional
    public InvestigationReport investigate(UUID incidentId) {
        RevenueIncident incident = incidents.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Revenue incident not found: " + incidentId));
        if (incident.getStatus() != RevenueIncidentStatus.DETECTED) {
            throw new IllegalStateException("Only DETECTED incidents can be investigated; current status is "
                    + incident.getStatus());
        }

        incident.transitionTo(stateMachine.transition(incident.getStatus(), RevenueIncidentStatus.INVESTIGATING));
        incidents.saveAndFlush(incident);
        Instant started = Instant.now();
        AgentContext context = new AgentContext(incidentId.toString(), started,
                started.plus(2, ChronoUnit.MINUTES), Map.of("maximumStatus", "DIAGNOSED"));

        AgentResult<TriageResult> triage = triageAgent.execute(incident, context);
        AgentResult<AnalystFindings> analyst = analystAgent.execute(incident, context);
        persistFinding(incident, FindingSource.PAYMENT_ANALYST, analyst.summary(),
                analyst.confidence().value(), analyst.output().evidence());

        List<SimilarHistoricalIncident> similar = memory.findSimilar(incident);
        RootCauseInput rootInput = new RootCauseInput(incident, triage.output(), analyst.output(), similar);
        AgentResult<RootCauseResult> diagnosis = rootCauseAgent.execute(rootInput, context);
        persistFinding(incident, FindingSource.ROOT_CAUSE_AGENT, diagnosis.summary(),
                diagnosis.confidence().value(), diagnosis.output().evidence());

        incident.transitionTo(stateMachine.transition(incident.getStatus(), RevenueIncidentStatus.DIAGNOSED));
        incidents.saveAndFlush(incident);
        return new InvestigationReport(incidentId, incident.getStatus(), triage.output(),
                analyst.output(), similar.size(), diagnosis.output());
    }

    private void persistFinding(RevenueIncident incident, FindingSource source, String summary,
                                double confidence, List<String> evidence) {
        findings.saveAndFlush(new IncidentFinding(incident, source, summary,
                BigDecimal.valueOf(confidence).setScale(4, RoundingMode.HALF_UP),
                evidence, Instant.now()));
    }
}
