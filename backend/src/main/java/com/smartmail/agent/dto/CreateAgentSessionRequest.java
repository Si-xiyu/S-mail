package com.smartmail.agent.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record CreateAgentSessionRequest(
        String scope,
        Map<String, Object> context,
        Long itemId,
        Long mailItemId
) {
    public Map<String, Object> resolvedContext() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (context != null) {
            result.putAll(context);
        }
        Long resolvedItemId = mailItemId != null ? mailItemId : itemId;
        if (resolvedItemId != null) {
            result.putIfAbsent("mailItemId", resolvedItemId);
        }
        return result;
    }
}