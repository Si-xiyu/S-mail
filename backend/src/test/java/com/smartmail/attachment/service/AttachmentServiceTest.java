package com.smartmail.attachment.service;

import com.smartmail.attachment.dto.PendingAttachmentResponse;
import com.smartmail.attachment.mapper.MailAttachmentMapper;
import com.smartmail.attachment.mapper.PendingAttachmentMapper;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.CurrentUser;
import com.smartmail.common.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "smartmail.attachment.storage-root=${java.io.tmpdir}/smartmail-attachment-test",
        "smartmail.attachment.max-file-size-bytes=1024"
})
class AttachmentServiceTest {
    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    private PendingAttachmentMapper pendingMapper;
    @Autowired
    private MailAttachmentMapper attachmentMapper;

    @TempDir
    Path tempDir;

    @AfterEach
    void clearUser() {
        UserContext.clear();
    }

    @Test
    void uploadAndBindCurrentUsersPendingAttachment() {
        UserContext.set(new CurrentUser(101L, "sender@smartmail.local", "Sender"));
        MockMultipartFile file = new MockMultipartFile("file", "report.txt", "text/plain", "hello".getBytes());

        PendingAttachmentResponse uploaded = attachmentService.upload(file);
        int bound = attachmentService.bindPendingAttachments(101L, 9001L, List.of(uploaded.pendingAttachmentId()));

        assertThat(bound).isEqualTo(1);
        assertThat(pendingMapper.selectById(uploaded.pendingAttachmentId()).getStatus()).isEqualTo("BOUND");
        assertThat(attachmentMapper.listByMailId(9001L)).hasSize(1);
    }

    @Test
    void uploadRejectsEmptyFile() {
        UserContext.set(new CurrentUser(101L, "sender@smartmail.local", "Sender"));
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> attachmentService.upload(file))
                .isInstanceOf(BusinessException.class);
    }
}
