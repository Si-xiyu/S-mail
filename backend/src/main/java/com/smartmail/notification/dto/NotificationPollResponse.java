package com.smartmail.notification.dto;

public record NotificationPollResponse(
        long unreadCount,
        long newMailCount,
        long inboxCount,
        long junkCount
) {
}
