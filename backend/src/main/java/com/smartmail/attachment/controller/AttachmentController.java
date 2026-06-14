package com.smartmail.attachment.controller;

import com.smartmail.attachment.dto.PendingAttachmentResponse;
import com.smartmail.attachment.service.AttachmentService;
import com.smartmail.common.response.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1")
public class AttachmentController {
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping("/compose/attachments")
    public ApiResponse<PendingAttachmentResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(attachmentService.upload(file));
    }

    @DeleteMapping("/compose/attachments/{pendingAttachmentId}")
    public ApiResponse<Void> deletePending(@PathVariable Long pendingAttachmentId) {
        attachmentService.deletePending(pendingAttachmentId);
        return ApiResponse.ok();
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long attachmentId) {
        AttachmentService.DownloadFile download = attachmentService.loadForDownload(attachmentId);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (download.mimeType() != null) {
            mediaType = MediaType.parseMediaType(download.mimeType());
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(download.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(download.resource());
    }
}
