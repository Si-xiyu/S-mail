package com.smartmail.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.smartmail.common.validation.SmartMailAddress;

public record LoginRequest(
        @Email @NotBlank
        @Pattern(regexp = SmartMailAddress.REGEX, message = "邮箱必须使用 @smail.com 后缀") String email,
        @NotBlank String password
) {
}
