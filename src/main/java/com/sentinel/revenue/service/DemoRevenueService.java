package com.sentinel.revenue.service;

import com.sentinel.revenue.api.BatchIngestionSummary;
import com.sentinel.revenue.api.DemoIncidentSummary;
import com.sentinel.revenue.api.DemoInjectionResponse;
import com.sentinel.revenue.api.DemoResetResponse;
import com.sentinel.revenue.dataset.SyntheticPaymentDatasetGenerator;
import com.sentinel.revenue.model.PaymentEvent;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.repository.IncidentFindingRepository;
import com.sentinel.revenue.repository.PaymentEventRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DemoRevenueService {

    private final PaymentEventIngestionService ingestionService;
    private final PaymentEventRepository paymentEvents;
    private final RevenueIncidentRepository incidents;
    private final IncidentFindingRepository findings;
    private final EntityManager entityManager;

    public DemoRevenueService(PaymentEventIngestionService ingestionService,
                              PaymentEventRepository paymentEvents,
                              RevenueIncidentRepository incidents,
                              IncidentFindingRepository findings,
                              EntityManager entityManager) {
        this.ingestionService = ingestionService;
        this.paymentEvents = paymentEvents;
        this.incidents = incidents;
        this.findings = findings;
        this.entityManager = entityManager;
    }

    @Transactional
    public DemoResetResponse resetSyntheticState() {
        List<PaymentEvent> syntheticEvents = paymentEvents.findAll().stream()
                .filter(event -> Boolean.TRUE.equals(event.getMetadata().get("synthetic")))
                .toList();
        Set<String> syntheticPaymentIds = syntheticEvents.stream()
                .map(PaymentEvent::getPaymentId)
                .collect(java.util.stream.Collectors.toSet());
        List<RevenueIncident> syntheticIncidents = incidents.findAll().stream()
                .filter(incident -> incident.getAffectedPayments().stream()
                        .anyMatch(syntheticPaymentIds::contains))
                .toList();

        List<UUID> incidentIds = syntheticIncidents.stream()
                .map(RevenueIncident::getIncidentId)
                .toList();
        if (!incidentIds.isEmpty()) {
            entityManager.createNativeQuery("select set_config('sentinel.demo_reset', 'true', true)")
                    .getSingleResult();
            deleteIncidentDependents(incidentIds);
            findings.deleteAllByIncidentIncidentIdIn(incidentIds);
            findings.flush();
            incidents.deleteAllInBatch(syntheticIncidents);
        }
        if (!syntheticEvents.isEmpty()) {
            paymentEvents.deleteAllInBatch(syntheticEvents);
        }
        return new DemoResetResponse(syntheticIncidents.size(), syntheticEvents.size());
    }

    @Transactional
    public DemoInjectionResponse injectUpiOutage() {
        Set<UUID> existingIncidentIds = new HashSet<>();
        incidents.findAll().forEach(incident -> existingIncidentIds.add(incident.getIncidentId()));

        BatchIngestionSummary ingestion = ingestionService.ingest(
                new SyntheticPaymentDatasetGenerator().generateScenario(
                        SyntheticPaymentDatasetGenerator.Scenario.UPI_DEGRADATION));
        List<DemoIncidentSummary> created = incidents.findAll().stream()
                .filter(incident -> !existingIncidentIds.contains(incident.getIncidentId()))
                .map(this::summary)
                .toList();
        return new DemoInjectionResponse(ingestion, created.size(), created);
    }

    private DemoIncidentSummary summary(RevenueIncident incident) {
        return new DemoIncidentSummary(
                incident.getIncidentId(),
                incident.getType(),
                incident.getAmountAtRiskMinor(),
                incident.getAffectedPayments().size());
    }

    /**
     * Demo reset is the sole maintenance path that removes derived synthetic state.
     * AuditEventRepository remains append-only and deliberately exposes no delete operation.
     */
    private void deleteIncidentDependents(List<UUID> incidentIds) {
        deleteByIncident("RecoveryOutcome", incidentIds);
        deleteByIncident("RecoveryAction", incidentIds);
        deleteByIncident("RecoveryPlan", incidentIds);
        entityManager.createQuery("delete from HistoricalIncident historical "
                        + "where historical.originalIncident.incidentId in :incidentIds")
                .setParameter("incidentIds", incidentIds)
                .executeUpdate();
        deleteByIncident("AuditEvent", incidentIds);
        entityManager.flush();
        entityManager.clear();
    }

    private void deleteByIncident(String entityName, List<UUID> incidentIds) {
        entityManager.createQuery("delete from " + entityName
                        + " entity where entity.incident.incidentId in :incidentIds")
                .setParameter("incidentIds", incidentIds)
                .executeUpdate();
    }
}
