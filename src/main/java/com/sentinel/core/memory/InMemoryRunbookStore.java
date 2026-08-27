package com.sentinel.core.memory;

import com.sentinel.core.config.RunbookProperties;
import com.sentinel.core.error.MemoryInitializationException;
import com.sentinel.core.llm.EmbeddingClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public class InMemoryRunbookStore implements RunbookMemory {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRunbookStore.class);

    private final EmbeddingClient embeddingClient;
    private final RunbookProperties properties;
    private volatile List<RunbookDocument> runbooks = List.of();

    public InMemoryRunbookStore(EmbeddingClient embeddingClient, RunbookProperties properties) {
        this.embeddingClient = embeddingClient;
        this.properties = properties;
    }

    @PostConstruct
    void loadRunbooks() {
        Path folder = properties.path().toAbsolutePath().normalize();
        List<RunbookDocument> loaded = new ArrayList<>();

        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".txt"))
                    .sorted()
                    .toList()) {
                String content = Files.readString(file);
                loaded.add(new RunbookDocument(
                        file.getFileName().toString(),
                        content,
                        embeddingClient.embed(content)
                ));
                log.info("Embedded runbook: {}", file.getFileName());
            }
        } catch (IOException exception) {
            throw new MemoryInitializationException("Could not load runbooks from " + folder, exception);
        }

        runbooks = List.copyOf(loaded);
        log.info("Loaded {} runbooks into memory", runbooks.size());
    }

    @Override
    public List<MemoryMatch> findSimilar(float[] queryEmbedding, int limit, double minimumSimilarity) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        if (minimumSimilarity < -1.0 || minimumSimilarity > 1.0) {
            throw new IllegalArgumentException("minimumSimilarity must be between -1.0 and 1.0");
        }

        return runbooks.stream()
                .map(document -> new MemoryMatch(
                        document,
                        CosineSimilarity.calculate(queryEmbedding, document.embedding())
                ))
                .filter(match -> match.similarity() >= minimumSimilarity)
                .sorted(Comparator.comparingDouble(MemoryMatch::similarity).reversed())
                .limit(limit)
                .toList();
    }
}
