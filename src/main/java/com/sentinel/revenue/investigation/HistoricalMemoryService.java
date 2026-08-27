package com.sentinel.revenue.investigation;

import com.sentinel.core.llm.EmbeddingClient;
import com.sentinel.core.memory.CosineSimilarity;
import com.sentinel.revenue.model.HistoricalIncident;
import com.sentinel.revenue.model.RevenueIncident;
import com.sentinel.revenue.repository.HistoricalIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class HistoricalMemoryService {
    private final HistoricalIncidentRepository repository;
    private final EmbeddingClient embeddings;
    private final InvestigationProperties properties;

    public HistoricalMemoryService(HistoricalIncidentRepository repository,
                                   EmbeddingClient embeddings,
                                   InvestigationProperties properties) {
        this.repository = repository;
        this.embeddings = embeddings;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<SimilarHistoricalIncident> findSimilar(RevenueIncident incident) {
        List<HistoricalIncident> history = repository.findAll();
        if (history.isEmpty()) return List.of();
        try {
            float[] query = embeddings.embed(incident.getType() + " " + String.join(" ", incident.getEvidence()));
            List<SimilarHistoricalIncident> matches = new ArrayList<>();
            for (HistoricalIncident item : history) {
                String text = item.getRootCause() + " " + item.getEvidenceSummary()
                        + " " + item.getRecoveryStrategy() + " " + item.getOutcome();
                double similarity = CosineSimilarity.calculate(query, embeddings.embed(text));
                if (similarity >= properties.memoryMinimumSimilarity()) {
                    long atRisk = CustomerContextTool.amountAtRisk(item);
                    Double rate = atRisk == 0 ? null : (double) item.getRecoveredAmountMinor() / atRisk;
                    matches.add(new SimilarHistoricalIncident(item.getId(), item.getRootCause(),
                            item.getRecoveryStrategy(), item.getOutcome(), item.getRecoveredAmountMinor(),
                            rate, similarity));
                }
            }
            return matches.stream().sorted(Comparator.comparingDouble(
                            SimilarHistoricalIncident::similarity).reversed())
                    .limit(properties.memoryTopK()).toList();
        } catch (RuntimeException unavailable) {
            return List.of();
        }
    }
}
