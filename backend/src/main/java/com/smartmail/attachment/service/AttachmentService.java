package com.smartmail.attachment.service;

import com.smartmail.attachment.config.AttachmentStorageConfig;
import com.smartmail.attachment.dto.AttachmentResponse;
import com.smartmail.attachment.dto.PendingAttachmentResponse;
import com.smartmail.attachment.entity.MailAttachment;
import com.smartmail.attachment.entity.PendingAttachment;
import com.smartmail.attachment.mapper.MailAttachmentMapper;
import com.smartmail.attachment.mapper.PendingAttachmentMapper;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.UserContext;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AttachmentService {

    private final AttachmentStorageConfig storageConfig;
    private final PendingAttachmentMapper pendingMapper;
    private final MailAttachmentMapper mailAttachmentMapper;
    private final MailboxItemMapper mailboxItemMapper;

    public AttachmentService(
            AttachmentStorageConfig storageConfig,
            PendingAttachmentMapper pendingMapper,
            MailAttachmentMapper mailAttachmentMapper,
            MailboxItemMapper mailboxItemMapper
    ) {
        this.storageConfig = storageConfig;
        this.pendingMapper = pendingMapper;
        this.mailAttachmentMapper = mailAttachmentMapper;
        this.mailboxItemMapper = mailboxItemMapper;
    }

    public PendingAttachmentResponse upload(MultipartFile file) {
        Long userId = UserContext.requireUserId();
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(400, "文件名不能为空");
        }

        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalName.substring(dot);
        }
        String storedName = UUID.randomUUID().toString() + ext;
        Path dest = storageConfig.getStoragePath().resolve(storedName);

        try {
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(500, "附件保存失败: " + e.getMessage());
        }

        String sha256 = computeSha256(dest);
        PendingAttachment entity = new PendingAttachment();
        entity.setUploaderId(userId);
        entity.setOriginalName(originalName);
        entity.setStoragePath(dest.toString());
        entity.setMimeType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setSha256(sha256);
        entity.setStatus("UPLOADED");
        entity.setCreatedAt(LocalDateTime.now());
        pendingMapper.insert(entity);

        return new PendingAttachmentResponse(
                entity.getId(),
                entity.getOriginalName(),
                entity.getMimeType(),
                entity.getFileSize(),
                entity.getStatus()
        );
    }

    public void removePending(Long pendingId) {
        Long userId = UserContext.requireUserId();
        PendingAttachment pending = pendingMapper.findOwnedPending(pendingId, userId);
        if (pending == null) {
            throw new BusinessException(404, "未找到待绑定附件");
        }
        try {
            Files.deleteIfExists(Path.of(pending.getStoragePath()));
        } catch (IOException ignored) {
        }
        pendingMapper.deleteById(pendingId);
    }

    public void bindToMail(Long mailId, List<Long> pendingIds) {
        Long userId = UserContext.requireUserId();
        if (pendingIds == null || pendingIds.isEmpty()) {
            return;
        }
        for (Long pendingId : pendingIds) {
            PendingAttachment pending = pendingMapper.findOwnedPending(pendingId, userId);
            if (pending == null) {
                throw new BusinessException(400, "待绑定附件不存在或无权操作: " + pendingId);
            }
            MailAttachment attachment = new MailAttachment();
            attachment.setMailId(mailId);
            attachment.setUploaderId(userId);
            attachment.setOriginalName(pending.getOriginalName());
            attachment.setStoragePath(pending.getStoragePath());
            attachment.setMimeType(pending.getMimeType());
            attachment.setFileSize(pending.getFileSize());
            attachment.setSha256(pending.getSha256());
            attachment.setCreatedAt(LocalDateTime.now());
            mailAttachmentMapper.insert(attachment);
            pendingMapper.deleteById(pendingId);
        }
    }

    public org.springframework.core.io.Resource download(Long attachmentId) {
        Long userId = UserContext.requireUserId();
        MailAttachment attachment = mailAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(404, "附件不存在");
        }
        if (attachment.getMailId() != null) {
            var item = mailboxItemMapper.findVisibleByUserAndMail(userId, attachment.getMailId());
            if (item == null) {
                throw new BusinessException(403, "无权下载该附件");
            }
        }
        Path filePath = Path.of(attachment.getStoragePath());
        if (!Files.exists(filePath)) {
            throw new BusinessException(404, "附件文件不存在");
        }
        return new org.springframework.core.io.InputStreamResource(
                () -> {
                    try {
                        return Files.newInputStream(filePath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        ) {
            @Override
            public String getFilename() {
                return attachment.getOriginalName();
            }
            @Override
            public long contentLength() {
                try {
                    return Files.size(filePath);
                } catch (IOException e) {
                    return attachment.getFileSize();
                }
            }
        };
    }

    public String getMimeType(Long attachmentId) {
        MailAttachment attachment = mailAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            return "application/octet-stream";
        }
        return attachment.getMimeType() != null ? attachment.getMimeType() : "application/octet-stream";
    }

    public List<AttachmentResponse> getAttachmentsForMail(Long mailId) {
        List<MailAttachment> attachments = mailAttachmentMapper.listByMailId(mailId);
        List<AttachmentResponse> result = new ArrayList<>();
        for (MailAttachment a : attachments) {
            result.add(new AttachmentResponse(
                    a.getId(),
                    a.getOriginalName(),
                    a.getMimeType(),
                    a.getFileSize(),
                    "/api/v1/attachments/" + a.getId() + "/download"
            ));
        }
        return result;
    }

    private String computeSha256(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(filePath)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = is.read(buf)) != -1) {
                    digest.update(buf, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            return null;
        }
    }
}
