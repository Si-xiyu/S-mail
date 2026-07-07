package com.smartmail.agent.controller;

import com.smartmail.agent.dto.AgentMessageRequest;
import com.smartmail.agent.dto.AgentMessageResponse;
import com.smartmail.agent.dto.AgentSessionResponse;
import com.smartmail.agent.dto.ConfirmAgentActionRequest;
import com.smartmail.agent.dto.ConfirmAgentActionResponse;
import com.smartmail.agent.dto.CreateAgentSessionRequest;
import com.smartmail.agent.service.AgentSessionService;
import com.smartmail.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentSessionController {
    private final AgentSessionService agentSessionService;

    public AgentSessionController(AgentSessionService agentSessionService) {
        this.agentSessionService = agentSessionService;
    }

    @PostMapping("/sessions")
    public ApiResponse<AgentSessionResponse> createSession(@RequestBody(required = false) CreateAgentSessionRequest request) {
        return ApiResponse.ok(agentSessionService.createSession(request));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<AgentSessionResponse> getSession(@PathVariable String sessionId) {
        return ApiResponse.ok(agentSessionService.getSession(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<AgentMessageResponse> sendMessage(
            @PathVariable String sessionId,
            @RequestBody AgentMessageRequest request
    ) {
        return ApiResponse.ok(agentSessionService.sendMessage(sessionId, request));
    }

    @PostMapping("/actions/{actionId}/confirm")
    public ApiResponse<ConfirmAgentActionResponse> confirmAction(
            @PathVariable String actionId,
            @RequestBody ConfirmAgentActionRequest request
    ) {
        return ApiResponse.ok(agentSessionService.confirmAction(actionId, request));
    }
}
