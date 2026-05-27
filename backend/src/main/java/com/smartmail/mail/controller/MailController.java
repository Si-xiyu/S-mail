package com.smartmail.mail.controller;

import com.smartmail.common.response.ApiResponse;
import com.smartmail.mail.dto.MailDetailResponse;
import com.smartmail.mail.dto.MailSendResponse;
import com.smartmail.mail.dto.SendMailRequest;
import com.smartmail.mail.service.MailService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mails")
public class MailController {
    private final MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping
    public ApiResponse<MailSendResponse> send(@Valid @RequestBody SendMailRequest request) {
        return ApiResponse.ok(mailService.send(request));
    }

    @GetMapping("/{mailId}")
    public ApiResponse<MailDetailResponse> detail(@PathVariable Long mailId) {
        return ApiResponse.ok(mailService.detail(mailId));
    }
}
