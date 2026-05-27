package com.smartmail.mailbox.controller;

import com.smartmail.common.response.ApiResponse;
import com.smartmail.common.response.PageResponse;
import com.smartmail.mailbox.dto.MailboxItemResponse;
import com.smartmail.mailbox.dto.ReadRequest;
import com.smartmail.mailbox.dto.StarRequest;
import com.smartmail.mailbox.service.MailboxService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mailbox")
public class MailboxController {
    private final MailboxService mailboxService;

    public MailboxController(MailboxService mailboxService) {
        this.mailboxService = mailboxService;
    }

    @GetMapping
    public ApiResponse<PageResponse<MailboxItemResponse>> list(
            @RequestParam(defaultValue = "INBOX") String folder,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        return ApiResponse.ok(mailboxService.list(folder, page, pageSize));
    }

    @PatchMapping("/items/{itemId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long itemId, @RequestBody ReadRequest request) {
        mailboxService.markRead(itemId, Boolean.TRUE.equals(request.read()));
        return ApiResponse.ok();
    }

    @PatchMapping("/items/{itemId}/star")
    public ApiResponse<Void> star(@PathVariable Long itemId, @RequestBody StarRequest request) {
        mailboxService.star(itemId, Boolean.TRUE.equals(request.starred()));
        return ApiResponse.ok();
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> delete(@PathVariable Long itemId) {
        mailboxService.delete(itemId);
        return ApiResponse.ok();
    }
}
