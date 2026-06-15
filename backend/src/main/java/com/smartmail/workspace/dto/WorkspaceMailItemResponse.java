package com.smartmail.workspace.dto;

import java.time.LocalDateTime;

public record WorkspaceMailItemResponse(
        Long itemId,
        Long mailId,
        String folder,
        String senderEmail,
        String subject,
        String summaryPreview,
        WorkspaceCategoryResponse category,
        String analysisStatus,
        Boolean read,
        Boolean starred,
        String priority,
        Boolean hasAttachment,
        LocalDateTime receivedAt
) {
}
