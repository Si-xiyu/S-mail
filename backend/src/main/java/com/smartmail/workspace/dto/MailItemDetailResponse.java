package com.smartmail.workspace.dto;

import com.smartmail.attachment.dto.AttachmentResponse;

import java.time.LocalDateTime;
import java.util.List;

public record MailItemDetailResponse(
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
        AnalysisSummary analysis,
        List<AttachmentResponse> attachments,
        AgentAvailability agent,
        LocalDateTime sentAt
) {
    public record AnalysisSummary(
            String status,
            List<String> summary,
            MailItemBriefResponse.CategoryBrief category,
            Boolean junk,
            List<String> riskHints
    ) {}

    public record AgentAvailability(boolean currentMailAgentAvailable) {}
}
