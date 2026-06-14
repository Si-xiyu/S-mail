package com.smartmail.user.dto;

public record UpdateUserSettingRequest(
        Boolean aiEnabled,
        Boolean agentAutoWriteEnabled
) {
}
