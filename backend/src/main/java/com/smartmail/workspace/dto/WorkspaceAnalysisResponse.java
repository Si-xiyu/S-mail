package com.smartmail.workspace.dto;

import java.util.List;

public record WorkspaceAnalysisResponse(
        String status,
        List<String> summary,
        WorkspaceCategoryResponse category,
        Boolean junk,
        List<String> riskHints
) {
}
