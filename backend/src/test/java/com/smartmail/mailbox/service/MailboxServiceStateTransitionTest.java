package com.smartmail.mailbox.service;

import com.smartmail.category.entity.MailCategory;
import com.smartmail.category.service.CategoryService;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.CurrentUser;
import com.smartmail.common.security.UserContext;
import com.smartmail.mail.mapper.MailMessageMapper;
import com.smartmail.mailbox.entity.MailboxItem;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailboxServiceStateTransitionTest {
    private MailboxItemMapper mailboxMapper;
    private CategoryService categoryService;
    private MailboxService service;

    @BeforeEach
    void setUp() {
        mailboxMapper = mock(MailboxItemMapper.class);
        categoryService = mock(CategoryService.class);
        service = new MailboxService(mailboxMapper, mock(MailMessageMapper.class), categoryService);
        UserContext.set(new CurrentUser(1L, "user@smail.com", "User"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void sentAndTrashItemsRejectNormalMoves() {
        MailboxItem sent = item(1L, "SENT");
        when(mailboxMapper.selectById(1L)).thenReturn(sent);
        assertThatThrownBy(() -> service.move(1L, "INBOX"))
                .isInstanceOf(BusinessException.class);

        MailboxItem trash = item(2L, "TRASH");
        trash.setOriginalFolder("INBOX");
        when(mailboxMapper.selectById(2L)).thenReturn(trash);
        assertThatThrownBy(() -> service.move(2L, "JUNK"))
                .isInstanceOf(BusinessException.class);

        verify(mailboxMapper, never()).updateById(sent);
        verify(mailboxMapper, never()).updateById(trash);
    }

    @Test
    void deleteAndRestorePreserveOriginalFolder() {
        MailboxItem item = item(3L, "SENT");
        when(mailboxMapper.selectById(3L)).thenReturn(item);

        service.delete(3L);
        assertThat(item.getFolder()).isEqualTo("TRASH");
        assertThat(item.getOriginalFolder()).isEqualTo("SENT");

        service.move(3L, "RESTORE");
        assertThat(item.getFolder()).isEqualTo("SENT");
        assertThat(item.getOriginalFolder()).isNull();
        verify(mailboxMapper, times(2)).updateById(item);
    }

    @Test
    void deletingFromTrashPermanentlyHidesOnlyTheMailboxItem() {
        MailboxItem item = item(4L, "TRASH");
        item.setOriginalFolder("INBOX");
        when(mailboxMapper.selectById(4L)).thenReturn(item);

        service.delete(4L);

        assertThat(item.getDeletedFlag()).isTrue();
        assertThat(item.getFolder()).isEqualTo("TRASH");
        verify(mailboxMapper).updateById(item);
    }

    @Test
    void movingJunkBackToInboxRemovesJunkLabel() {
        MailboxItem item = item(5L, "JUNK");
        MailCategory junk = category(40L, CategoryService.DEFAULT_JUNK);
        MailCategory other = category(41L, CategoryService.DEFAULT_OTHER);
        when(mailboxMapper.selectById(5L)).thenReturn(item);
        when(categoryService.findDefaultCategory(1L, CategoryService.DEFAULT_JUNK)).thenReturn(junk);
        when(categoryService.findDefaultCategory(1L, CategoryService.DEFAULT_OTHER)).thenReturn(other);
        when(categoryService.getCategoryForMail(item.getMailId(), 1L)).thenReturn(other);

        service.move(5L, "INBOX");

        assertThat(item.getFolder()).isEqualTo("INBOX");
        verify(categoryService).removeCategoryForUser(item.getMailId(), 1L, 40L);
        verify(categoryService, never()).assignCategory(item.getMailId(), 41L, "MANUAL");
    }

    @Test
    void markingInboxMailReadDoesNotMoveOrDeleteIt() {
        MailboxItem item = item(6L, "INBOX");
        when(mailboxMapper.selectById(6L)).thenReturn(item);

        service.markRead(6L, true);

        assertThat(item.getReadFlag()).isTrue();
        assertThat(item.getFolder()).isEqualTo("INBOX");
        assertThat(item.getDeletedFlag()).isFalse();
        verify(mailboxMapper).updateById(item);
    }

    @Test
    void readingTrashMailDoesNotPostponeRetentionDeadline() {
        LocalDateTime trashedAt = LocalDateTime.of(2026, 6, 1, 12, 0);
        MailboxItem item = item(7L, "TRASH");
        item.setUpdatedAt(trashedAt);
        when(mailboxMapper.selectById(7L)).thenReturn(item);

        service.markRead(7L, true);

        assertThat(item.getUpdatedAt()).isEqualTo(trashedAt);
        verify(mailboxMapper).updateById(item);
    }

    @Test
    void purgeExpiredTrashUsesProvidedThirtyDayCutoff() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 6, 7, 3, 0);
        when(mailboxMapper.softDeleteExpiredTrash(eq(cutoff), any(LocalDateTime.class))).thenReturn(2);

        assertThat(service.purgeExpiredTrash(cutoff)).isEqualTo(2);

        verify(mailboxMapper).softDeleteExpiredTrash(eq(cutoff), any(LocalDateTime.class));
    }

    private MailboxItem item(Long id, String folder) {
        MailboxItem item = new MailboxItem();
        item.setId(id);
        item.setUserId(1L);
        item.setMailId(900L + id);
        item.setFolder(folder);
        item.setDeletedFlag(false);
        return item;
    }

    private MailCategory category(Long id, String name) {
        MailCategory category = new MailCategory();
        category.setId(id);
        category.setUserId(1L);
        category.setName(name);
        return category;
    }
}
