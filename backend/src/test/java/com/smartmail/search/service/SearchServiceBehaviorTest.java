package com.smartmail.search.service;

import com.smartmail.common.security.CurrentUser;
import com.smartmail.common.security.UserContext;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceBehaviorTest {

    private MailboxItemMapper mailboxItemMapper;
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        mailboxItemMapper = mock(MailboxItemMapper.class);
        searchService = new SearchService(mailboxItemMapper);
        UserContext.set(new CurrentUser(1L, "user@smail.com", "User"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void searchNormalizesFolderAndPaginationLikeMailboxList() {
        when(mailboxItemMapper.searchByKeyword(1L, "%project%", "INBOX", 7L, true, 50L, 0L)).thenReturn(List.of());
        when(mailboxItemMapper.countByKeyword(1L, "%project%", "INBOX", 7L, true)).thenReturn(0L);

        searchService.search("project", "inbox", 7L, true, 0, 500);

        verify(mailboxItemMapper).searchByKeyword(1L, "%project%", "INBOX", 7L, true, 50L, 0L);
        verify(mailboxItemMapper).countByKeyword(1L, "%project%", "INBOX", 7L, true);
    }

    @Test
    void searchMapsLowercaseDatabaseColumnNames() {
        LocalDateTime receivedAt = LocalDateTime.of(2026, 7, 6, 20, 0);
        Map<String, Object> row = Map.ofEntries(
                Map.entry("id", 21L),
                Map.entry("mail_id", 31L),
                Map.entry("sender_email", "sender@example.com"),
                Map.entry("subject", "Project update"),
                Map.entry("content_text", "Current project status"),
                Map.entry("folder", "INBOX"),
                Map.entry("read_flag", false),
                Map.entry("star_flag", true),
                Map.entry("priority", "HIGH"),
                Map.entry("has_attachment", true),
                Map.entry("received_at", receivedAt)
        );
        when(mailboxItemMapper.searchByKeyword(1L, "%project%", null, null, null, 20L, 0L))
                .thenReturn(List.of(row));
        when(mailboxItemMapper.countByKeyword(1L, "%project%", null, null, null)).thenReturn(1L);

        var result = searchService.search("project", null, null, null, 1, 20);

        assertThat(result.records()).singleElement().satisfies(record -> {
            assertThat(record.itemId()).isEqualTo(21L);
            assertThat(record.mailId()).isEqualTo(31L);
            assertThat(record.senderEmail()).isEqualTo("sender@example.com");
            assertThat(record.subject()).isEqualTo("Project update");
            assertThat(record.folder()).isEqualTo("INBOX");
            assertThat(record.priority()).isEqualTo("HIGH");
            assertThat(record.receivedAt()).isEqualTo(receivedAt);
        });
    }
}
