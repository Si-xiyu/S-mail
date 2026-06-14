package com.smartmail.workspace.controller;

import com.smartmail.common.response.ApiResponse;
import com.smartmail.common.response.PageResponse;
import com.smartmail.workspace.dto.WorkspaceMailDetailResponse;
import com.smartmail.workspace.dto.WorkspaceMailItemResponse;
import com.smartmail.workspace.dto.WorkspaceViewsResponse;
import com.smartmail.workspace.service.WorkspaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/views")
    public ApiResponse<WorkspaceViewsResponse> views() {
        return ApiResponse.ok(workspaceService.views());
    }

    @GetMapping("/mail-items")
    public ApiResponse<PageResponse<WorkspaceMailItemResponse>> mailItems(
            @RequestParam(defaultValue = "inbox") String view,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        return ApiResponse.ok(workspaceService.list(view, categoryId, keyword, page, pageSize));
    }

    @GetMapping("/mail-items/{itemId}")
    public ApiResponse<WorkspaceMailDetailResponse> detail(@PathVariable Long itemId) {
        return ApiResponse.ok(workspaceService.detail(itemId));
    }
}
