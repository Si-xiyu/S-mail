package com.smartmail.attachment.dto;

public record AttachmentResponse(
        Long id,
        String fileName,
        String mimeType,
        Long fileSize,
        String downloadUrl
) {}
