package com.smartmail.notification.controller;

import com.smartmail.common.response.ApiResponse;
import com.smartmail.notification.dto.NotificationPollResponse;
import com.smartmail.notification.service.NotificationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/poll")
    public ApiResponse<NotificationPollResponse> poll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since
    ) {
        return ApiResponse.ok(notificationService.poll(since));
    }
}
