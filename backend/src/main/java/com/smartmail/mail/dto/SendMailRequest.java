package com.smartmail.mail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import com.smartmail.common.validation.SmartMailAddress;

import java.util.List;

public record SendMailRequest(
        @NotEmpty List<@NotBlank @Email @Pattern(
                regexp = SmartMailAddress.REGEX,
                message = "邮箱必须使用 @smail.com 后缀"
        ) String> to,
        List<@NotBlank @Email @Pattern(
                regexp = SmartMailAddress.REGEX,
                message = "邮箱必须使用 @smail.com 后缀"
        ) String> cc,
        List<@NotBlank @Email @Pattern(
                regexp = SmartMailAddress.REGEX,
                message = "邮箱必须使用 @smail.com 后缀"
        ) String> bcc,
        @NotBlank String subject,
        @NotBlank String contentText,
        String contentHtml,
        List<Long> pendingAttachmentIds,
        Long parentMailId
) {
}
