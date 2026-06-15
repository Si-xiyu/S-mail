package com.smartmail.search.dto;

import java.time.LocalDateTime;

public record SearchResultResponse(
        Long itemId,
        Long mailId,
        String senderEmail,
        String subject,
        String snippet,
        String folder,
        Boolean read,
        Boolean starred,
        String priority,
        Boolean hasAttachment,
        LocalDateTime receivedAt
) {}
