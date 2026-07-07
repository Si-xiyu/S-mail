package com.smartmail.agent.dto;

public record AgentMessageRequest(String message, String content) {
    public String resolvedMessage() {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return content;
    }
}