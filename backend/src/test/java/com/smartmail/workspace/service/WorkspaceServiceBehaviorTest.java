package com.smartmail.workspace.service;

import com.smartmail.ai.mapper.MailAiResultMapper;
import com.smartmail.attachment.mapper.MailAttachmentMapper;
import com.smartmail.category.mapper.MailCategoryAssignmentMapper;
import com.smartmail.category.mapper.MailCategoryMapper;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.CurrentUser;
import com.smartmail.common.security.UserContext;
import com.smartmail.mail.entity.MailMessage;
import com.smartmail.mail.mapper.MailMessageMapper;
import com.smartmail.mail.mapper.MailRecipientMapper;
import com.smartmail.mailbox.entity.MailboxItem;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkspaceServiceBehaviorTest {

    private MailboxItemMapper mailboxItemMapper;
    private MailMessageMapper mailMessageMapper;
    private MailRecipientMapper recipientMapper;
    private MailAiResultMapper aiResultMapper;
    private MailCategoryMapper categoryMapper;
    private MailCategoryAssignmentMapper assignmentMapper;
    private MailAttachmentMapper attachmentMapper;
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        mailboxItemMapper = mock(MailboxItemMapper.class);
        mailMessageMapper = mock(MailMessageMapper.class);
        recipientMapper = mock(MailRecipientMapper.class);
        aiResultMapper = mock(MailAiResultMapper.class);
        categoryMapper = mock(MailCategoryMapper.class);
        assignmentMapper = mock(MailCategoryAssignmentMapper.class);
        attachmentMapper = mock(MailAttachmentMapper.class);
        workspaceService = new WorkspaceService(
                mailboxItemMapper,
                mailMessageMapper,
                recipientMapper,
                aiResultMapper,
                categoryMapper,
                assignmentMapper,
                attachmentMapper
        );
        UserContext.set(new CurrentUser(1L, "user@smartmail.local", "User"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void listMailItemsSecondPageKeepsMapperPageInsteadOfPaginatingTwice() {
        var item3 = mailboxItem(3L, 103L);
        var item4 = mailboxItem(4L, 104L);
        when(mailboxItemMapper.listByFolder(1L, "INBOX", 2L, 2L)).thenReturn(List.of(item3, item4));
        when(mailboxItemMapper.countByFolder(1L, "INBOX")).thenReturn(4L);
        when(mailMessageMapper.selectBatchIds(anySet())).thenReturn(List.of(
                mailMessage(103L, "Third mail"),
                mailMessage(104L, "Fourth mail")
        ));
        when(aiResultMapper.listByMailAndUser(103L, 1L)).thenReturn(List.of());
        when(aiResultMapper.listByMailAndUser(104L, 1L)).thenReturn(List.of());
        when(assignmentMapper.findByMailAndUser(103L, 1L)).thenReturn(null);
        when(assignmentMapper.findByMailAndUser(104L, 1L)).thenReturn(null);

        var page = workspaceService.listMailItems("inbox", null, null, 2, 2);

        assertThat(page.records())
                .extracting("itemId")
                .containsExactly(3L, 4L);
        assertThat(page.total()).isEqualTo(4L);
    }

    @Test
    void getMailItemDetailRejectsDeletedMailboxItem() {
        var deleted = mailboxItem(9L, 109L);
        deleted.setDeletedFlag(true);
        when(mailboxItemMapper.selectById(9L)).thenReturn(deleted);
        when(mailMessageMapper.selectById(109L)).thenReturn(mailMessage(109L, "Deleted mail"));
        when(recipientMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(aiResultMapper.listByMailAndUser(109L, 1L)).thenReturn(List.of());
        when(assignmentMapper.findByMailAndUser(109L, 1L)).thenReturn(null);
        when(attachmentMapper.listByMailId(109L)).thenReturn(List.of());

        assertThatThrownBy(() -> workspaceService.getMailItemDetail(9L))
                .isInstanceOf(BusinessException.class);
    }

    private MailboxItem mailboxItem(Long itemId, Long mailId) {
        MailboxItem item = new MailboxItem();
        item.setId(itemId);
        item.setUserId(1L);
        item.setMailId(mailId);
        item.setFolder("INBOX");
        item.setReadFlag(false);
        item.setStarFlag(false);
        item.setDeletedFlag(false);
        item.setPriority("NORMAL");
        item.setReceivedAt(LocalDateTime.now());
        return item;
    }

    private MailMessage mailMessage(Long id, String subject) {
        MailMessage message = new MailMessage();
        message.setId(id);
        message.setSenderEmail("sender@smartmail.local");
        message.setSubject(subject);
        message.setContentText("Message body for " + subject);
        message.setHasAttachment(false);
        message.setSentAt(LocalDateTime.now());
        return message;
    }
}
