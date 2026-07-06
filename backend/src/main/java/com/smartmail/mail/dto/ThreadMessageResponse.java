package com.smartmail.mail.dto;

import java.time.LocalDateTime;

public record ThreadMessageResponse(
        Long mailId,
        String senderEmail,
        String subject,
        String contentText,
        LocalDateTime sentAt,
        Long parentMailId,
        Long threadId
) {
}
