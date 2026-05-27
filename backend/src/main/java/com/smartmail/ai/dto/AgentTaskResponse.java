package com.smartmail.ai.dto;

import java.util.Map;

public record AgentTaskResponse(String task, String status, Map<String, Object> result) {
}
