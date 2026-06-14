package com.smartmail.workspace.dto;

import java.util.List;

public record WorkspaceViewsResponse(
        List<WorkspaceCountResponse> views,
        List<WorkspaceCountResponse> folders,
        List<WorkspaceCategoryResponse> categories
) {
}
