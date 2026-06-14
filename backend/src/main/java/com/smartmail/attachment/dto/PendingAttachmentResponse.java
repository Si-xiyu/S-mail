package com.smartmail.attachment.dto;

public record PendingAttachmentResponse(
        Long pendingAttachmentId,
        String fileName,
        String mimeType,
        Long fileSize,
        String status
) {
}
