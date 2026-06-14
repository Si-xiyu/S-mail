package com.smartmail.workspace.controller;

import com.smartmail.common.response.ApiResponse;
import com.smartmail.common.response.PageResponse;
import com.smartmail.workspace.dto.MailItemBriefResponse;
import com.smartmail.workspace.dto.MailItemDetailResponse;
import com.smartmail.workspace.dto.WorkspaceViewsResponse;
import com.smartmail.workspace.service.WorkspaceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/views")
    public ApiResponse<WorkspaceViewsResponse> views() {
        return ApiResponse.ok(workspaceService.getViews());
    }

    @GetMapping("/mail-items")
    public ApiResponse<PageResponse<MailItemBriefResponse>> listMailItems(
            @RequestParam(defaultValue = "inbox") String view,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        return ApiResponse.ok(workspaceService.listMailItems(view, categoryId, keyword, page, pageSize));
    }

    @GetMapping("/mail-items/{itemId}")
    public ApiResponse<MailItemDetailResponse> mailItemDetail(@PathVariable Long itemId) {
        return ApiResponse.ok(workspaceService.getMailItemDetail(itemId));
    }
}
