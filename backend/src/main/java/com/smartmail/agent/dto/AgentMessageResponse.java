package com.smartmail.agent.dto;

import java.util.List;

public record AgentMessageResponse(
        String sessionId,
        String status,
        String answer,
        AgentMessageView assistantMessage,
        List<PendingAgentActionResponse> pendingActions
) {}
