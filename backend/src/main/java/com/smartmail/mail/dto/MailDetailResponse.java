package com.smartmail.mail.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record MailDetailResponse(
        Long mailId,
        Long itemId,
        String messageNo,
        String senderEmail,
        String subject,
        String contentText,
        String contentHtml,
        String folder,
        Boolean read,
        Boolean starred,
        String priority,
        LocalDateTime sentAt,
        List<String> recipients,
        List<Map<String, Object>> aiResults
) {
}
