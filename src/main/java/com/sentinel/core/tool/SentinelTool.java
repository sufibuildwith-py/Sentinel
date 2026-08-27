package com.sentinel.core.tool;

import com.sentinel.core.agent.AgentContext;

public interface SentinelTool<I, O> {

    O execute(I input, AgentContext context);
}
