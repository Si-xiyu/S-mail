package com.smartmail.attachment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartmail.attachment.config.AttachmentStorageProperties;
import com.smartmail.attachment.dto.AttachmentResponse;
import com.smartmail.attachment.dto.PendingAttachmentResponse;
import com.smartmail.attachment.entity.MailAttachment;
import com.smartmail.attachment.entity.PendingAttachment;
import com.smartmail.attachment.mapper.MailAttachmentMapper;
import com.smartmail.attachment.mapper.PendingAttachmentMapper;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.UserContext;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AttachmentService {
    private final PendingAttachmentMapper pendingMapper;
    private final MailAttachmentMapper attachmentMapper;
    private final MailboxItemMapper mailboxMapper;
    private final AttachmentStorageProperties properties;

    public AttachmentService(
            PendingAttachmentMapper pendingMapper,
            MailAttachmentMapper attachmentMapper,
            MailboxItemMapper mailboxMapper,
            AttachmentStorageProperties properties
    ) {
        this.pendingMapper = pendingMapper;
        this.attachmentMapper = attachmentMapper;
        this.mailboxMapper = mailboxMapper;
        this.properties = properties;
    }

    @Transactional
    public PendingAttachmentResponse upload(MultipartFile file) {
        Long userId = UserContext.requireUserId();
        validateUpload(file);
        LocalDateTime now = LocalDateTime.now();
        String originalName = cleanFileName(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "-" + originalName;
        Path target = storageRoot().resolve(userId.toString()).resolve(storedName).normalize();
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException ex) {
            throw new BusinessException(500, "Failed to store attachment");
        }

        PendingAttachment pending = new PendingAttachment();
        pending.setUploaderId(userId);
        pending.setOriginalName(originalName);
        pending.setStoragePath(target.toString());
        pending.setMimeType(file.getContentType());
        pending.setFileSize(file.getSize());
        pending.setSha256(sha256(target));
        pending.setStatus("UPLOADED");
        pending.setCreatedAt(now);
        pending.setUpdatedAt(now);
        pendingMapper.insert(pending);
        return toPendingResponse(pending);
    }

    @Transactional
    public void deletePending(Long pendingAttachmentId) {
        Long userId = UserContext.requireUserId();
        PendingAttachment pending = pendingMapper.selectById(pendingAttachmentId);
        if (pending == null || !userId.equals(pending.getUploaderId()) || !"UPLOADED".equals(pending.getStatus())) {
            throw new BusinessException(404, "Pending attachment not found");
        }
        pending.setStatus("DELETED");
        pending.setUpdatedAt(LocalDateTime.now());
        pendingMapper.updateById(pending);
        try {
            Files.deleteIfExists(Path.of(pending.getStoragePath()));
        } catch (IOException ignored) {
        }
    }

    @Transactional
    public int bindPendingAttachments(Long uploaderId, Long mailId, List<Long> pendingAttachmentIds) {
        if (pendingAttachmentIds == null || pendingAttachmentIds.isEmpty()) {
            return 0;
        }
        List<Long> distinctIds = pendingAttachmentIds.stream().distinct().toList();
        List<PendingAttachment> pendingAttachments = pendingMapper.selectList(
                new QueryWrapper<PendingAttachment>()
                        .eq("uploader_id", uploaderId)
                        .eq("status", "UPLOADED")
                        .in("id", distinctIds)
        );
        if (pendingAttachments.size() != distinctIds.size()) {
            throw new BusinessException(400, "Invalid pending attachment");
        }
        LocalDateTime now = LocalDateTime.now();
        for (PendingAttachment pending : pendingAttachments.stream().sorted(Comparator.comparing(PendingAttachment::getId)).toList()) {
            MailAttachment attachment = new MailAttachment();
            attachment.setMailId(mailId);
            attachment.setUploaderId(uploaderId);
            attachment.setOriginalName(pending.getOriginalName());
            attachment.setStoragePath(pending.getStoragePath());
            attachment.setMimeType(pending.getMimeType());
            attachment.setFileSize(pending.getFileSize());
            attachment.setSha256(pending.getSha256());
            attachment.setCreatedAt(now);
            attachmentMapper.insert(attachment);

            pending.setStatus("BOUND");
            pending.setBoundMailId(mailId);
            pending.setUpdatedAt(now);
            pendingMapper.updateById(pending);
        }
        return pendingAttachments.size();
    }

    public List<AttachmentResponse> listMailAttachments(Long mailId) {
        return attachmentMapper.listByMailId(mailId).stream().map(this::toAttachmentResponse).toList();
    }

    public DownloadFile loadForDownload(Long attachmentId) {
        Long userId = UserContext.requireUserId();
        MailAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null || mailboxMapper.findVisibleByUserAndMail(userId, attachment.getMailId()) == null) {
            throw new BusinessException(404, "Attachment not found");
        }
        Path file = Path.of(attachment.getStoragePath()).normalize();
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new BusinessException(404, "Attachment file not found");
        }
        return new DownloadFile(new FileSystemResource(file), attachment.getOriginalName(), attachment.getMimeType(), attachment.getFileSize());
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException(400, "Attachment file is empty");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BusinessException(400, "Attachment file is too large");
        }
    }

    private String cleanFileName(String originalFilename) {
        String fileName = StringUtils.cleanPath(originalFilename == null ? "attachment" : originalFilename);
        if (!StringUtils.hasText(fileName) || Set.of(".", "..").contains(fileName)) {
            return "attachment";
        }
        return fileName.replace('\\', '_').replace('/', '_');
    }

    private Path storageRoot() {
        return Path.of(properties.getStorageRoot()).toAbsolutePath().normalize();
    }

    private String sha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file);
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                digestInput.transferTo(OutputStreamNull.INSTANCE);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new BusinessException(500, "Failed to checksum attachment");
        }
    }

    private PendingAttachmentResponse toPendingResponse(PendingAttachment pending) {
        return new PendingAttachmentResponse(
                pending.getId(),
                pending.getOriginalName(),
                pending.getMimeType(),
                pending.getFileSize(),
                pending.getStatus()
        );
    }

    private AttachmentResponse toAttachmentResponse(MailAttachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalName(),
                attachment.getMimeType(),
                attachment.getFileSize(),
                "/api/v1/attachments/" + attachment.getId() + "/download"
        );
    }

    public record DownloadFile(Resource resource, String fileName, String mimeType, Long fileSize) {
    }

    private static final class OutputStreamNull extends java.io.OutputStream {
        private static final OutputStreamNull INSTANCE = new OutputStreamNull();

        @Override
        public void write(int b) {
        }
    }
}
