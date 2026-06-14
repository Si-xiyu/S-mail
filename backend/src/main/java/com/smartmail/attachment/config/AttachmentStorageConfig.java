package com.smartmail.attachment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class AttachmentStorageConfig {

    private final Path storagePath;
    private final long maxFileSizeBytes;

    public AttachmentStorageConfig(
            @Value("${smartmail.attachment.storage-root:${smartmail.storage.attachment-path:./storage/attachments}}") String path,
            @Value("${smartmail.attachment.max-file-size-bytes:52428800}") long maxFileSizeBytes
    ) {
        this.storagePath = Path.of(path).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
        try {
            Files.createDirectories(this.storagePath);
        } catch (IOException e) {
            throw new RuntimeException("无法创建附件存储目录: " + this.storagePath, e);
        }
    }

    public Path getStoragePath() {
        return storagePath;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }
}
