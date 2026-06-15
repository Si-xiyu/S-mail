package com.smartmail.internal.dto;

import java.util.List;

public record InternalMailResponse(
        Long itemId,
        Long mailId,
        Long userId,
        String folder,
        String senderEmail,
        String subject,
        String contentText,
        String contentHtml,
        String priority,
        List<String> recipients
) {
}
