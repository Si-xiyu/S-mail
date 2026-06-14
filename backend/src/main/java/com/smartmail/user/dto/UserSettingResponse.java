package com.smartmail.user.dto;

public record UserSettingResponse(
        Boolean aiEnabled,
        Boolean agentAutoWriteEnabled
) {
}
