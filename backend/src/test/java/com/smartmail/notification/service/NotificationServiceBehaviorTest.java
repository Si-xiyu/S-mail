package com.smartmail.notification.service;

import com.smartmail.common.security.CurrentUser;
import com.smartmail.common.security.UserContext;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceBehaviorTest {
    private MailboxItemMapper mailboxMapper;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        mailboxMapper = mock(MailboxItemMapper.class);
        service = new NotificationService(mailboxMapper);
        UserContext.set(new CurrentUser(1L, "user@smartmail.local", "User"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void pollReportsInboxAndJunkCountsWithoutUsingSentOrTrash() {
        LocalDateTime since = LocalDateTime.of(2026, 7, 6, 12, 0);
        when(mailboxMapper.countUnread(1L)).thenReturn(3L);
        when(mailboxMapper.countSince(eq(1L), eq(since), any(LocalDateTime.class))).thenReturn(2L);
        when(mailboxMapper.countByFolder(1L, "INBOX")).thenReturn(8L);
        when(mailboxMapper.countByFolder(1L, "JUNK")).thenReturn(1L);

        LocalDateTime before = LocalDateTime.now();
        var result = service.poll(since);
        LocalDateTime after = LocalDateTime.now();

        assertThat(result.unreadCount()).isEqualTo(3L);
        assertThat(result.newMailCount()).isEqualTo(2L);
        assertThat(result.inboxCount()).isEqualTo(8L);
        assertThat(result.junkCount()).isEqualTo(1L);
        assertThat(result.cursor()).isBetween(before, after);
        ArgumentCaptor<LocalDateTime> cursorCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mailboxMapper).countSince(eq(1L), eq(since), cursorCaptor.capture());
        assertThat(cursorCaptor.getValue()).isEqualTo(result.cursor());
        verify(mailboxMapper).countByFolder(1L, "INBOX");
        verify(mailboxMapper).countByFolder(1L, "JUNK");
    }
}
