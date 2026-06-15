package com.smartmail.workspace.dto;

public record WorkspaceAttachmentResponse(
        Long id,
        String fileName,
        String mimeType,
        Long fileSize,
        String downloadUrl
) {
}
