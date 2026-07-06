package com.smartmail.notification.dto;

import java.time.LocalDateTime;

public record NotificationPollResponse(
        long unreadCount,
        long newMailCount,
        long inboxCount,
        long junkCount,
        LocalDateTime cursor
) {
}
