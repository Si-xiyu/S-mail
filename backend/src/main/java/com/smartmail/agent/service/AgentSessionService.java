package com.smartmail.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmail.agent.dto.AgentMessageRequest;
import com.smartmail.agent.dto.AgentMessageResponse;
import com.smartmail.agent.dto.AgentMessageView;
import com.smartmail.agent.dto.AgentSessionResponse;
import com.smartmail.agent.dto.ConfirmAgentActionRequest;
import com.smartmail.agent.dto.ConfirmAgentActionResponse;
import com.smartmail.agent.dto.CreateAgentSessionRequest;
import com.smartmail.agent.dto.PendingAgentActionResponse;
import com.smartmail.agent.entity.AgentMessage;
import com.smartmail.agent.entity.AgentPendingAction;
import com.smartmail.agent.entity.AgentSession;
import com.smartmail.agent.mapper.AgentMessageMapper;
import com.smartmail.agent.mapper.AgentPendingActionMapper;
import com.smartmail.agent.mapper.AgentSessionMapper;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.UserContext;
import com.smartmail.internal.dto.InternalMailActionRequest;
import com.smartmail.internal.service.InternalToolService;
import com.smartmail.user.entity.UserSetting;
import com.smartmail.user.service.UserSettingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AgentSessionService {
    private final AgentSessionMapper sessionMapper;
    private final AgentMessageMapper messageMapper;
    private final AgentPendingActionMapper actionMapper;
    private final UserSettingService userSettingService;
    private final InternalToolService internalToolService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String agentBaseUrl;
    private final boolean agentEnabled;
    private final String pluginToken;

    public AgentSessionService(
            AgentSessionMapper sessionMapper,
            AgentMessageMapper messageMapper,
            AgentPendingActionMapper actionMapper,
            UserSettingService userSettingService,
            InternalToolService internalToolService,
            ObjectMapper objectMapper,
            RestTemplateBuilder builder,
            @Value("${smartmail.ai.agent-base-url}") String agentBaseUrl,
            @Value("${smartmail.ai.enabled}") boolean agentEnabled,
            @Value("${smartmail.ai.timeout-seconds}") long timeoutSeconds,
            @Value("${smartmail.ai.plugin-token:smartmail-agent-plugin-dev-token}") String pluginToken
    ) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.actionMapper = actionMapper;
        this.userSettingService = userSettingService;
        this.internalToolService = internalToolService;
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.agentBaseUrl = agentBaseUrl;
        this.agentEnabled = agentEnabled;
        this.pluginToken = pluginToken;
    }

    @Transactional
    public AgentSessionResponse createSession(CreateAgentSessionRequest request) {
        Long userId = UserContext.requireUserId();
        String scope = normalizeScope(request == null ? null : request.scope());
        Map<String, Object> context = request == null || request.context() == null ? Map.of() : request.context();
        LocalDateTime now = LocalDateTime.now();

        AgentSession session = new AgentSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setScope(scope);
        session.setContextJson(toJson(context));
        session.setStatus("ACTIVE");
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionMapper.insert(session);
        return toSessionResponse(session);
    }

    public AgentSessionResponse getSession(String sessionId) {
        Long userId = UserContext.requireUserId();
        AgentSession session = requireSession(sessionId, userId);
        return toSessionResponse(session);
    }

    @Transactional
    public AgentMessageResponse sendMessage(String sessionId, AgentMessageRequest request) {
        Long userId = UserContext.requireUserId();
        AgentSession session = requireSession(sessionId, userId);
        String message = request == null ? null : request.message();
        if (message == null || message.isBlank()) {
            throw new BusinessException(400, "message is required");
        }

        saveMessage(sessionId, userId, "USER", message, "SUCCEEDED", List.of());
        UserSetting setting = userSettingService.ensure(userId);
        if (!agentEnabled || !Boolean.TRUE.equals(setting.getAiEnabled())) {
            AgentMessage assistant = saveMessage(
                    sessionId,
                    userId,
                    "ASSISTANT",
                    "AI Plugin 已关闭，系统处于基础邮箱模式。",
                    "DISABLED",
                    List.of()
            );
            return new AgentMessageResponse(sessionId, "DISABLED", assistant.getContent(), toMessageView(assistant), List.of());
        }

        Map<String, Object> pluginResponse = callPlugin(session, userId, message, setting);
        String status = stringValue(pluginResponse.get("status"), "FAILED");
        String answer = stringValue(pluginResponse.get("answer"), "Agent Plugin 未返回回答。");
        List<Map<String, Object>> toolCalls = listOfMaps(pluginResponse.get("toolCalls"));
        List<Map<String, Object>> pendingActions = listOfMaps(pluginResponse.get("pendingActions"));

        AgentMessage assistant = saveMessage(sessionId, userId, "ASSISTANT", answer, status, toolCalls);
        List<PendingAgentActionResponse> savedActions = savePendingActions(sessionId, userId, pendingActions);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        return new AgentMessageResponse(sessionId, status, answer, toMessageView(assistant), savedActions);
    }

    @Transactional
    public ConfirmAgentActionResponse confirmAction(String actionId, ConfirmAgentActionRequest request) {
        Long userId = UserContext.requireUserId();
        AgentPendingAction action = actionMapper.findByActionIdAndUserId(actionId, userId);
        if (action == null) {
            throw new BusinessException(404, "Agent action not found");
        }
        if (!"PENDING".equals(action.getStatus())) {
            return new ConfirmAgentActionResponse(action.getActionId(), action.getStatus(), "Action is not pending");
        }
        if (request == null || !Boolean.TRUE.equals(request.confirmed())) {
            action.setStatus("CANCELLED");
            action.setUpdatedAt(LocalDateTime.now());
            actionMapper.updateById(action);
            return new ConfirmAgentActionResponse(action.getActionId(), "CANCELLED", "Action cancelled");
        }

        Map<String, Object> payload = jsonToMap(action.getPayloadJson());
        payload.put("userId", userId);
        payload.putIfAbsent("action", action.getType());
        InternalMailActionRequest internalRequest = objectMapper.convertValue(payload, InternalMailActionRequest.class);
        internalToolService.executeAction(internalRequest);
        action.setStatus("EXECUTED");
        action.setUpdatedAt(LocalDateTime.now());
        actionMapper.updateById(action);
        return new ConfirmAgentActionResponse(action.getActionId(), "EXECUTED", "Action executed by backend");
    }

    private Map<String, Object> callPlugin(AgentSession session, Long userId, String message, UserSetting setting) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("sessionId", session.getSessionId());
        request.put("userId", userId);
        request.put("scope", session.getScope());
        request.put("message", message);
        request.put("context", jsonToMap(session.getContextJson()));
        request.put("toolPolicy", Map.of("agentAutoWriteEnabled", Boolean.TRUE.equals(setting.getAgentAutoWriteEnabled())));
        request.put("pluginConfig", Map.of("aiPluginEnabled", Boolean.TRUE.equals(setting.getAiEnabled())));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Plugin-Token", pluginToken);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    agentBaseUrl + "/plugin/v1/agent/chat",
                    new HttpEntity<>(request, headers),
                    Map.class
            );
            if (response == null) {
                throw new BusinessException(502, "Agent Plugin 返回为空");
            }
            return response;
        } catch (RestClientException ex) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("status", "FAILED");
            fallback.put("answer", "Agent Plugin 暂不可用，基础邮件功能不受影响。");
            fallback.put("toolCalls", List.of(Map.of("tool", "agent_plugin", "status", "FAILED", "error", ex.getMessage())));
            fallback.put("pendingActions", List.of());
            return fallback;
        }
    }

    private AgentSession requireSession(String sessionId, Long userId) {
        AgentSession session = sessionMapper.findBySessionIdAndUserId(sessionId, userId);
        if (session == null) {
            throw new BusinessException(404, "Agent session not found");
        }
        return session;
    }

    private AgentMessage saveMessage(
            String sessionId,
            Long userId,
            String role,
            String content,
            String status,
            List<Map<String, Object>> toolCalls
    ) {
        AgentMessage message = new AgentMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setStatus(status);
        message.setToolCallsJson(toJson(toolCalls));
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    private List<PendingAgentActionResponse> savePendingActions(
            String sessionId,
            Long userId,
            List<Map<String, Object>> pendingActions
    ) {
        List<PendingAgentActionResponse> responses = new ArrayList<>();
        for (Map<String, Object> item : pendingActions) {
            String actionId = stringValue(item.get("actionId"), null);
            if (actionId == null || actionId.isBlank()) {
                continue;
            }
            AgentPendingAction action = new AgentPendingAction();
            action.setActionId(actionId);
            action.setSessionId(sessionId);
            action.setUserId(userId);
            action.setType(stringValue(item.get("type"), "UNKNOWN"));
            action.setLabel(stringValue(item.get("label"), action.getType()));
            action.setPayloadJson(toJson(mapValue(item.get("payload"))));
            action.setReason(stringValue(item.get("reason"), ""));
            action.setStatus(stringValue(item.get("status"), "PENDING"));
            action.setExecution(stringValue(item.get("execution"), "BACKEND_REQUIRED"));
            action.setCreatedAt(LocalDateTime.now());
            action.setUpdatedAt(LocalDateTime.now());
            actionMapper.insert(action);
            responses.add(toPendingActionResponse(action));
        }
        return responses;
    }

    private AgentSessionResponse toSessionResponse(AgentSession session) {
        Long userId = session.getUserId();
        List<AgentMessageView> messages = messageMapper.listBySessionAndUser(session.getSessionId(), userId)
                .stream()
                .map(this::toMessageView)
                .toList();
        List<PendingAgentActionResponse> actions = actionMapper.listBySessionAndUser(session.getSessionId(), userId)
                .stream()
                .map(this::toPendingActionResponse)
                .toList();
        return new AgentSessionResponse(
                session.getSessionId(),
                session.getScope(),
                jsonToMap(session.getContextJson()),
                session.getStatus(),
                messages,
                actions,
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private AgentMessageView toMessageView(AgentMessage message) {
        return new AgentMessageView(
                message.getRole(),
                message.getContent(),
                message.getStatus(),
                jsonToList(message.getToolCallsJson()),
                message.getCreatedAt()
        );
    }

    private PendingAgentActionResponse toPendingActionResponse(AgentPendingAction action) {
        return new PendingAgentActionResponse(
                action.getActionId(),
                action.getType(),
                action.getLabel(),
                jsonToMap(action.getPayloadJson()),
                action.getReason(),
                action.getStatus(),
                action.getExecution()
        );
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "GLOBAL";
        }
        String normalized = scope.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("GLOBAL") && !normalized.equals("CURRENT_MAIL")) {
            throw new BusinessException(400, "scope must be GLOBAL or CURRENT_MAIL");
        }
        return normalized;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "Invalid JSON payload");
        }
    }

    private Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }

    private List<Map<String, Object>> jsonToList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add(copyMap(map));
            }
        }
        return result;
    }

    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }
}
