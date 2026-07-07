package com.smartmail.agent.dto;

import java.util.Map;

public record CreateAgentSessionRequest(
        String scope,
        Map<String, Object> context
) {}
