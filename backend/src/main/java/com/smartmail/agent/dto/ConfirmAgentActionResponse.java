package com.smartmail.agent.dto;

public record ConfirmAgentActionResponse(
        String actionId,
        String status,
        String message
) {}
