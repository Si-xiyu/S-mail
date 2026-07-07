package com.smartmail.agent.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AgentSessionResponse(
        String sessionId,
        String scope,
        Map<String, Object> context,
        String status,
        List<AgentMessageView> messages,
        List<PendingAgentActionResponse> pendingActions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
