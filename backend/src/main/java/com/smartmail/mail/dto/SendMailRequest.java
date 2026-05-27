package com.smartmail.mail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SendMailRequest(
        @NotEmpty List<String> to,
        List<String> cc,
        @NotBlank String subject,
        @NotBlank String contentText,
        String contentHtml
) {
}
