package com.smartmail.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InternalMailActionRequest(
        @NotNull Long userId,
        @NotNull Long itemId,
        @NotBlank String action,
        String value
) {
}
