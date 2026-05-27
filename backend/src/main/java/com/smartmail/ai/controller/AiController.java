package com.smartmail.ai.controller;

import com.smartmail.ai.dto.AgentTaskResponse;
import com.smartmail.ai.service.AiService;
import com.smartmail.common.response.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/mails")
public class AiController {
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/{mailId}/summary")
    public ApiResponse<AgentTaskResponse> summary(@PathVariable Long mailId) {
        return ApiResponse.ok(aiService.runMailTask(mailId, "summary"));
    }

    @PostMapping("/{mailId}/reply-draft")
    public ApiResponse<AgentTaskResponse> replyDraft(@PathVariable Long mailId) {
        return ApiResponse.ok(aiService.runMailTask(mailId, "reply_draft"));
    }

    @PostMapping("/{mailId}/analyze")
    public ApiResponse<AgentTaskResponse> analyze(@PathVariable Long mailId) {
        return ApiResponse.ok(aiService.runMailTask(mailId, "analyze"));
    }
}
