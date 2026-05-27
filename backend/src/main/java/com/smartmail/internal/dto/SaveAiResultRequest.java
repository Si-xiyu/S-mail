package com.smartmail.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveAiResultRequest(
        @NotNull Long mailId,
        @NotNull Long userId,
        @NotBlank String resultType,
        @NotBlank String resultJson,
        @NotBlank String status
) {
}
