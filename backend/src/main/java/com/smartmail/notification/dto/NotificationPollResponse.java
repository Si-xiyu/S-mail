package com.smartmail.notification.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationPollResponse(
        long unreadCount,
        long newMailCount,
        long inboxCount,
        long junkCount,
        Map<String, Long> unreadByLabel,
        LocalDateTime cursor
) {
}
