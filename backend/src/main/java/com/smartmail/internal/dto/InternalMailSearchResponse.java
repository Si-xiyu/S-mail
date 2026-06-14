package com.smartmail.internal.dto;

import java.time.LocalDateTime;

public record InternalMailSearchResponse(
        Long itemId,
        Long mailId,
        String folder,
        String senderEmail,
        String subject,
        String preview,
        String priority,
        Boolean read,
        Boolean starred,
        LocalDateTime receivedAt
) {
}
