package com.smartmail.search.service;

import com.smartmail.common.security.CurrentUser;
import com.smartmail.common.security.UserContext;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        UserContext.set(new CurrentUser(1L, "user@smartmail.local", "User"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void searchNormalizesFolderAndPaginationLikeMailboxList() {
        when(mailboxItemMapper.searchByKeyword(1L, "%project%", "INBOX", 50L, 0L)).thenReturn(List.of());
        when(mailboxItemMapper.countByKeyword(1L, "%project%", "INBOX")).thenReturn(0L);

        searchService.search("project", "inbox", 0, 500);

        verify(mailboxItemMapper).searchByKeyword(1L, "%project%", "INBOX", 50L, 0L);
        verify(mailboxItemMapper).countByKeyword(1L, "%project%", "INBOX");
    }
}
