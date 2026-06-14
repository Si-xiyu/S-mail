package com.smartmail.workspace.dto;

import java.util.List;

public record WorkspaceMailDetailResponse(
        Long itemId,
        Long mailId,
        String folder,
        String senderEmail,
        List<String> recipients,
        String subject,
        String contentText,
        String contentHtml,
        Boolean read,
        Boolean starred,
        String priority,
        WorkspaceAnalysisResponse analysis,
        List<WorkspaceAttachmentResponse> attachments,
        WorkspaceAgentResponse agent
) {
}
