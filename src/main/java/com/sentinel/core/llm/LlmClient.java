package com.sentinel.core.llm;

public interface LlmClient {

    <T> T generateStructured(Prompt prompt, Class<T> outputType);
}
