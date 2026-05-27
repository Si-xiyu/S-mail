package com.smartmail.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SetPriorityRequest(@NotNull Long userId, @NotBlank String priority) {
}
