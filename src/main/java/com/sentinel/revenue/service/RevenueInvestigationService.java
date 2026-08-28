package com.sentinel.revenue.service;

import com.sentinel.core.agent.AgentContext;
import com.sentinel.core.agent.AgentResult;
import com.sentinel.revenue.investigation.*;
import com.sentinel.revenue.audit.AuditLogService;
import com.sentinel.revenue.model.FindingSource;
import com.sentinel.revenue.model.IncidentFinding;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.model.RevenueIncidentStatus;
import com.sentinel.revenue.repository.IncidentFindingRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AuditLogService audit;
    private final RevenueIncidentStateMachine stateMachine = new RevenueIncidentStateMachine();

    @Autowired
    public RevenueInvestigationService(RevenueIncidentRepository incidents,
                                       IncidentFindingRepository findings,
                                       TriageAgent triageAgent,
                                       PaymentAnalystAgent analystAgent,
                                       HistoricalMemoryService memory,
                                       RootCauseAgent rootCauseAgent,
                                       AuditLogService audit) {
        this.incidents = incidents;
        this.findings = findings;
        this.triageAgent = triageAgent;
        this.analystAgent = analystAgent;
        this.memory = memory;
        this.rootCauseAgent = rootCauseAgent;
        this.audit = audit;
    }

    public RevenueInvestigationService(RevenueIncidentRepository incidents,
                                       IncidentFindingRepository findings,
                                       TriageAgent triageAgent,
                                       PaymentAnalystAgent analystAgent,
                                       HistoricalMemoryService memory,
                                       RootCauseAgent rootCauseAgent) {
        this(incidents, findings, triageAgent, analystAgent, memory, rootCauseAgent, null);
    }

    @Transactional
    public InvestigationReport investigate(UUID incidentId) {
        RevenueIncident incident = incidents.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Revenue incident not found: " + incidentId));
        if (incident.getStatus() != RevenueIncidentStatus.DETECTED) {
            throw new IllegalStateException("Only DETECTED incidents can be investigated; current status is "
                    + incident.getStatus());
        }

        transition(incident, RevenueIncidentStatus.INVESTIGATING, "Agentic investigation started");
        Instant started = Instant.now();
        AgentContext context = new AgentContext(incidentId.toString(), started,
                started.plus(2, ChronoUnit.MINUTES), Map.of("maximumStatus", "DIAGNOSED"));

        AgentResult<TriageResult> triage = triageAgent.execute(incident, context);
        auditAgent(incident, triage);
        AgentResult<AnalystFindings> analyst = analystAgent.execute(incident, context);
        auditAgent(incident, analyst);
        persistFinding(incident, FindingSource.PAYMENT_ANALYST, analyst.summary(),
                analyst.confidence().value(), analyst.output().evidence());

        List<SimilarHistoricalIncident> similar = memory.findSimilar(incident);
        RootCauseInput rootInput = new RootCauseInput(incident, triage.output(), analyst.output(), similar);
        AgentResult<RootCauseResult> diagnosis = rootCauseAgent.execute(rootInput, context);
        auditAgent(incident, diagnosis);
        persistFinding(incident, FindingSource.ROOT_CAUSE_AGENT, diagnosis.summary(),
                diagnosis.confidence().value(), diagnosis.output().evidence());

        transition(incident, RevenueIncidentStatus.DIAGNOSED, diagnosis.output().rootCause());
        return new InvestigationReport(incidentId, incident.getStatus(), triage.output(),
                analyst.output(), similar.size(), diagnosis.output());
    }

    private void transition(RevenueIncident incident, RevenueIncidentStatus target, String outcome) {
        RevenueIncidentStatus previous = incident.getStatus();
        incident.transitionTo(stateMachine.transition(previous, target));
        incidents.saveAndFlush(incident);
        if (audit != null) audit.append(incident, "SENTINEL", null, "STATE_TRANSITION",
                List.of(), null, outcome, List.of(), null, previous, target, outcome);
    }

    private void auditAgent(RevenueIncident incident, AgentResult<?> result) {
        if (audit != null) audit.append(incident, "SENTINEL", result.agentName(), "AGENT_RESULT",
                result.evidence().stream().map(com.sentinel.core.agent.Evidence::description).toList(),
                BigDecimal.valueOf(result.confidence().value()).setScale(4, RoundingMode.HALF_UP),
                result.summary(), List.of(), null, null, null, result.status().name());
    }

    private void persistFinding(RevenueIncident incident, FindingSource source, String summary,
                                double confidence, List<String> evidence) {
        findings.saveAndFlush(new IncidentFinding(incident, source, summary,
                BigDecimal.valueOf(confidence).setScale(4, RoundingMode.HALF_UP),
                evidence, Instant.now()));
    }
}
