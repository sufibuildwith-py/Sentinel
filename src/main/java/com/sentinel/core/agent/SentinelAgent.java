package com.sentinel.core.agent;

public interface SentinelAgent<I, O> {

    AgentResult<O> execute(I input, AgentContext context);
}
