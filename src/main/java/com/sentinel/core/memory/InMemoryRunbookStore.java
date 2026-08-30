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
    private volatile List<RawRunbook> rawRunbooks = List.of();
    private volatile List<RunbookDocument> runbooks = List.of();

    public InMemoryRunbookStore(EmbeddingClient embeddingClient, RunbookProperties properties) {
        this.embeddingClient = embeddingClient;
        this.properties = properties;
    }

    @PostConstruct
    void loadRunbooks() {
        Path folder = properties.path().toAbsolutePath().normalize();
        List<RawRunbook> loaded = new ArrayList<>();

        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".txt"))
                    .sorted()
                    .toList()) {
                String content = Files.readString(file);
                loaded.add(new RawRunbook(file.getFileName().toString(), content));
                log.info("Loaded runbook: {}", file.getFileName());
            }
        } catch (IOException exception) {
            throw new MemoryInitializationException("Could not load runbooks from " + folder, exception);
        }

        rawRunbooks = List.copyOf(loaded);
        log.info("Loaded {} runbooks; embeddings will be initialized on first retrieval", rawRunbooks.size());
    }

    @Override
    public List<MemoryMatch> findSimilar(float[] queryEmbedding, int limit, double minimumSimilarity) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        if (minimumSimilarity < -1.0 || minimumSimilarity > 1.0) {
            throw new IllegalArgumentException("minimumSimilarity must be between -1.0 and 1.0");
        }

        return embeddedRunbooks().stream()
                .map(document -> new MemoryMatch(
                        document,
                        CosineSimilarity.calculate(queryEmbedding, document.embedding())
                ))
                .filter(match -> match.similarity() >= minimumSimilarity)
                .sorted(Comparator.comparingDouble(MemoryMatch::similarity).reversed())
                .limit(limit)
                .toList();
    }

    private List<RunbookDocument> embeddedRunbooks() {
        List<RunbookDocument> current = runbooks;
        if (current.size() == rawRunbooks.size()) {
            return current;
        }

        synchronized (this) {
            if (runbooks.size() == rawRunbooks.size()) {
                return runbooks;
            }

            List<RunbookDocument> embedded = rawRunbooks.stream()
                    .map(runbook -> new RunbookDocument(
                            runbook.source(),
                            runbook.content(),
                            embeddingClient.embed(runbook.content())
                    ))
                    .toList();
            runbooks = List.copyOf(embedded);
            log.info("Initialized embeddings for {} runbooks", runbooks.size());
            return runbooks;
        }
    }

    private record RawRunbook(String source, String content) {
    }
}
