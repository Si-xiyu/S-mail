package com.smartmail.agent.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AgentMessageView(
        String role,
        String content,
        String status,
        List<Map<String, Object>> toolCalls,
        LocalDateTime createdAt
) {}
