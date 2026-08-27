package com.sentinel.core.llm;

public interface EmbeddingClient {

    float[] embed(String text);
}
