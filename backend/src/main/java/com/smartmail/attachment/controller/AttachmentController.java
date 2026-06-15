package com.smartmail.attachment.controller;

import com.smartmail.attachment.dto.PendingAttachmentResponse;
import com.smartmail.attachment.service.AttachmentService;
import com.smartmail.common.response.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ApiResponse<Void> removePending(@PathVariable Long pendingAttachmentId) {
        attachmentService.removePending(pendingAttachmentId);
        return ApiResponse.ok();
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Resource resource = attachmentService.download(id);
        String mimeType = attachmentService.getMimeType(id);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        String filename = resource.getFilename() != null ? resource.getFilename() : "attachment";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename).build().toString())
                .body(resource);
    }
}
