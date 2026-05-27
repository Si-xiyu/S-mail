package com.smartmail.ai.service;

import com.smartmail.ai.dto.AgentTaskRequest;
import com.smartmail.ai.dto.AgentTaskResponse;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AiService {
    private final RestTemplate restTemplate;
    private final String agentBaseUrl;
    private final boolean enabled;

    public AiService(
            RestTemplateBuilder builder,
            @Value("${smartmail.ai.agent-base-url}") String agentBaseUrl,
            @Value("${smartmail.ai.enabled}") boolean enabled,
            @Value("${smartmail.ai.timeout-seconds}") long timeoutSeconds
    ) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.agentBaseUrl = agentBaseUrl;
        this.enabled = enabled;
    }

    public AgentTaskResponse runMailTask(Long mailId, String task) {
        Long userId = UserContext.requireUserId();
        if (!enabled) {
            return disabledResponse(task);
        }
        try {
            AgentTaskResponse response = restTemplate.postForObject(
                    agentBaseUrl + "/api/v1/agent/tasks",
                    new AgentTaskRequest(mailId, userId, task),
                    AgentTaskResponse.class
            );
            if (response == null) {
                throw new BusinessException(502, "Agent 返回为空");
            }
            return response;
        } catch (RestClientException ex) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Agent 暂不可用，基础邮件功能不受影响");
            result.put("reason", ex.getMessage());
            return new AgentTaskResponse(task, "FALLBACK", result);
        }
    }

    private AgentTaskResponse disabledResponse(String task) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "AI 功能未开启");
        return new AgentTaskResponse(task, "DISABLED", result);
    }
}
