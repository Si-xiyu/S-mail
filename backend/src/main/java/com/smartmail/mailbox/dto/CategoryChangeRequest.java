package com.smartmail.mailbox.dto;

import jakarta.validation.constraints.NotNull;

public record CategoryChangeRequest(@NotNull Long categoryId) {}
