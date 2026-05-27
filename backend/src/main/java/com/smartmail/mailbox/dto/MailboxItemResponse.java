package com.smartmail.mailbox.dto;

import java.time.LocalDateTime;

public record MailboxItemResponse(
        Long itemId,
        Long mailId,
        String senderEmail,
        String subject,
        String preview,
        String folder,
        Boolean read,
        Boolean starred,
        String priority,
        Boolean hasAttachment,
        LocalDateTime receivedAt
) {
}
