package com.sentinel.core.memory;

import java.util.List;

public interface RunbookMemory {

    List<MemoryMatch> findSimilar(float[] queryEmbedding, int limit, double minimumSimilarity);
}
