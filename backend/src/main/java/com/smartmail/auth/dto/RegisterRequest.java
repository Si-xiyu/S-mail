package com.smartmail.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.smartmail.common.validation.SmartMailAddress;

public record RegisterRequest(
        @Email @NotBlank
        @Pattern(regexp = SmartMailAddress.REGEX, message = "邮箱必须使用 @smail.com 后缀") String email,
        @NotBlank @Size(min = 2, max = 32) String username,
        @NotBlank @Size(min = 6, max = 64) String password
) {
}
