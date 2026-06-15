package com.smartmail.internal.dto;

import java.time.LocalDateTime;

public record InternalMailSearchResponse(
        Long itemId,
        Long mailItemId,
        Long mailId,
        String folder,
        String senderEmail,
        String subject,
        String preview,
        String snippet,
        Double score,
        String source,
        String priority,
        Boolean read,
        Boolean starred,
        LocalDateTime receivedAt
) {
}
