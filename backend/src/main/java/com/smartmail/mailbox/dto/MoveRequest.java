package com.smartmail.mailbox.dto;

import jakarta.validation.constraints.NotBlank;

public record MoveRequest(@NotBlank String folder) {}
