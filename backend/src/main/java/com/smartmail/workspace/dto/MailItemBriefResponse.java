package com.smartmail.workspace.dto;

import java.time.LocalDateTime;

public record MailItemBriefResponse(
        Long itemId,
        Long mailId,
        String folder,
        String senderEmail,
        String subject,
        String summaryPreview,
        CategoryBrief category,
        String analysisStatus,
        Boolean read,
        Boolean starred,
        String priority,
        Boolean hasAttachment,
        LocalDateTime receivedAt
) {
    public record CategoryBrief(Long id, String name, String color) {}
}
