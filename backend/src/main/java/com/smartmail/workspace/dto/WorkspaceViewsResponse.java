package com.smartmail.workspace.dto;

import java.util.List;

public record WorkspaceViewsResponse(
        List<ViewItem> views,
        List<FolderItem> folders,
        List<CategoryItem> categories
) {}
