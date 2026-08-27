package com.sentinel.core.memory;

import com.sentinel.core.config.RunbookProperties;
import com.sentinel.core.llm.EmbeddingClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InMemoryRunbookStoreTest {

    @TempDir
    Path runbookFolder;

    @Test
    void loadsRanksAndFiltersRunbooks() throws Exception {
        String checkout = "Checkout fails after a discount deployment.";
        String database = "Database CPU spikes during an analytics job.";
        Files.writeString(runbookFolder.resolve("checkout.txt"), checkout);
        Files.writeString(runbookFolder.resolve("database.txt"), database);

        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        when(embeddingClient.embed(checkout)).thenReturn(new float[]{1.0f, 0.0f});
        when(embeddingClient.embed(database)).thenReturn(new float[]{0.0f, 1.0f});

        InMemoryRunbookStore store = new InMemoryRunbookStore(
                embeddingClient,
                new RunbookProperties(runbookFolder, 1, -1.0)
        );
        store.loadRunbooks();

        List<MemoryMatch> matches = store.findSimilar(new float[]{0.9f, 0.1f}, 2, 0.5);

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).document().source()).isEqualTo("checkout.txt");
        assertThat(matches.get(0).similarity()).isGreaterThan(0.99);
    }
}
