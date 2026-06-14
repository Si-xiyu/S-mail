package com.smartmail.attachment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class AttachmentStorageConfig {

    private final Path storagePath;

    public AttachmentStorageConfig(@Value("${smartmail.storage.attachment-path}") String path) {
        this.storagePath = Path.of(path).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storagePath);
        } catch (IOException e) {
            throw new RuntimeException("无法创建附件存储目录: " + this.storagePath, e);
        }
    }

    public Path getStoragePath() {
        return storagePath;
    }
}
