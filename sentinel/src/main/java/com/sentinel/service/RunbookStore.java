package com.sentinel.service;

import com.sentinel.dto.RunbookEntry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

// This is our "vector store" - just an in-memory list for now.
// Good enough for a handful of runbooks; a real deployment would swap this
// for something like pgvector, but the retrieval logic below stays the same.
@Service
public class RunbookStore {

    @Value("${runbooks.path}")
    private String runbooksPath;

    private final EmbeddingService embeddingService;
    private final List<RunbookEntry> runbooks = new ArrayList<>();

    public RunbookStore(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    // Runs once when the app starts up - reads every .txt file, embeds it, and keeps it in memory.
    // This means startup will take a few seconds longer (one API call per runbook), which is fine.
    @PostConstruct
    public void loadAndEmbedRunbooks() throws IOException {
        Path folder = Path.of(runbooksPath);

        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".txt")).toList()) {
                String content = Files.readString(file);
                float[] vector = embeddingService.embed(content);
                runbooks.add(new RunbookEntry(file.getFileName().toString(), content, vector));
                System.out.println("Embedded runbook: " + file.getFileName());
            }
        }

        System.out.println("Loaded " + runbooks.size() + " runbooks into the vector store");
    }

    // Given an incident's vector, return the topK most similar runbooks
    public List<RunbookEntry> findMostSimilar(float[] queryVector, int topK) {
        return runbooks.stream()
                .sorted(Comparator.comparingDouble(
                        (RunbookEntry entry) -> cosineSimilarity(queryVector, entry.getVector())
                ).reversed())
                .limit(topK)
                .toList();
    }

    // Cosine similarity: measures the angle between two vectors, ignoring their length.
    // Result ranges from -1 (opposite meaning) to 1 (identical meaning). Closer to 1 = more similar.
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
