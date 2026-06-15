package com.smartmail.ai.service;

import com.smartmail.ai.dto.AgentTaskRequest;
import com.smartmail.ai.dto.AgentTaskResponse;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
    private final String pluginToken;

    public AiService(
            RestTemplateBuilder builder,
            @Value("${smartmail.ai.agent-base-url}") String agentBaseUrl,
            @Value("${smartmail.ai.enabled}") boolean enabled,
            @Value("${smartmail.ai.timeout-seconds}") long timeoutSeconds,
            @Value("${smartmail.ai.plugin-token:smartmail-agent-plugin-dev-token}") String pluginToken
    ) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.agentBaseUrl = agentBaseUrl;
        this.enabled = enabled;
        this.pluginToken = pluginToken;
    }

    public AgentTaskResponse runMailTask(Long mailId, String task) {
        Long userId = UserContext.requireUserId();
        return runMailTask(mailId, userId, task);
    }

    public AgentTaskResponse runMailTask(Long mailId, Long userId, String task) {
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

    public Map<String, Object> analyzeMail(Map<String, Object> request) {
        if (!enabled) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "DISABLED");
            result.put("message", "AI Plugin is disabled");
            return result;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Plugin-Token", pluginToken);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    agentBaseUrl + "/plugin/v1/analysis/mail",
                    new HttpEntity<>(request, headers),
                    Map.class
            );
            if (response == null) {
                throw new BusinessException(502, "Agent Plugin 返回为空");
            }
            return response;
        } catch (RestClientException ex) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "FAILED");
            result.put("errorCode", "PLUGIN_UNAVAILABLE");
            result.put("message", ex.getMessage());
            result.put("fallbackAvailable", true);
            return result;
        }
    }

    private AgentTaskResponse disabledResponse(String task) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "AI 功能未开启");
        return new AgentTaskResponse(task, "DISABLED", result);
    }
}
