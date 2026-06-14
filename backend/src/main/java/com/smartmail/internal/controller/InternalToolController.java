package com.smartmail.internal.controller;

import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.response.ApiResponse;
import com.smartmail.internal.dto.InternalMailActionRequest;
import com.smartmail.internal.dto.InternalMailResponse;
import com.smartmail.internal.dto.InternalMailSearchResponse;
import com.smartmail.internal.dto.SaveAiResultRequest;
import com.smartmail.internal.dto.SetPriorityRequest;
import com.smartmail.internal.service.InternalToolService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/v1/tools")
public class InternalToolController {
    private final InternalToolService internalToolService;
    private final String internalToken;

    public InternalToolController(InternalToolService internalToolService, @Value("${smartmail.internal-token}") String internalToken) {
        this.internalToolService = internalToolService;
        this.internalToken = internalToken;
    }

    @GetMapping("/mails/{mailId}")
    public ApiResponse<InternalMailResponse> getMail(
            @RequestHeader("X-Internal-Token") String token,
            @PathVariable Long mailId,
            @RequestParam Long userId
    ) {
        verify(token);
        return ApiResponse.ok(internalToolService.getMail(mailId, userId));
    }

    @GetMapping("/mail-items/{itemId}/context")
    public ApiResponse<InternalMailResponse> getMailContext(
            @RequestHeader("X-Internal-Token") String token,
            @PathVariable Long itemId,
            @RequestParam Long userId
    ) {
        verify(token);
        return ApiResponse.ok(internalToolService.getMailContext(itemId, userId));
    }

    @GetMapping("/mail-search")
    public ApiResponse<List<InternalMailSearchResponse>> search(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String folder,
            @RequestParam(defaultValue = "10") long limit
    ) {
        verify(token);
        return ApiResponse.ok(internalToolService.search(userId, keyword, folder, limit));
    }

    @PostMapping("/mails/{mailId}/priority")
    public ApiResponse<Void> setPriority(
            @RequestHeader("X-Internal-Token") String token,
            @PathVariable Long mailId,
            @Valid @RequestBody SetPriorityRequest request
    ) {
        verify(token);
        internalToolService.setPriority(mailId, request);
        return ApiResponse.ok();
    }

    @PostMapping("/ai-results")
    public ApiResponse<Void> saveAiResult(
            @RequestHeader("X-Internal-Token") String token,
            @Valid @RequestBody SaveAiResultRequest request
    ) {
        verify(token);
        internalToolService.saveAiResult(request);
        return ApiResponse.ok();
    }

    @PostMapping("/analysis-results")
    public ApiResponse<Void> saveAnalysisResult(
            @RequestHeader("X-Internal-Token") String token,
            @Valid @RequestBody SaveAiResultRequest request
    ) {
        verify(token);
        internalToolService.saveAiResult(request);
        return ApiResponse.ok();
    }

    @PostMapping("/mail-actions")
    public ApiResponse<Void> mailAction(
            @RequestHeader("X-Internal-Token") String token,
            @Valid @RequestBody InternalMailActionRequest request
    ) {
        verify(token);
        internalToolService.executeAction(request);
        return ApiResponse.ok();
    }

    @PostMapping("/mail-actions/execute")
    public ApiResponse<Void> executeMailAction(
            @RequestHeader("X-Internal-Token") String token,
            @Valid @RequestBody InternalMailActionRequest request
    ) {
        verify(token);
        internalToolService.executeAction(request);
        return ApiResponse.ok();
    }

    private void verify(String token) {
        if (!internalToken.equals(token)) {
            throw new BusinessException(403, "无效 internal token");
        }
    }
}
