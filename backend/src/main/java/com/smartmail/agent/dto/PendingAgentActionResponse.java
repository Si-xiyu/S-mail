package com.smartmail.agent.dto;

import java.util.Map;

public record PendingAgentActionResponse(
        String actionId,
        String type,
        String label,
        Map<String, Object> payload,
        String reason,
        String status,
        String execution
) {}
