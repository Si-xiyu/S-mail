package com.smartmail.internal.service;

import com.smartmail.ai.entity.MailAiResult;
import com.smartmail.ai.mapper.MailAiResultMapper;
import com.smartmail.category.service.CategoryService;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.internal.dto.InternalMailActionRequest;
import com.smartmail.internal.dto.SaveAiResultRequest;
import com.smartmail.mail.mapper.MailMessageMapper;
import com.smartmail.mail.mapper.MailRecipientMapper;
import com.smartmail.mailbox.entity.MailboxItem;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalToolServiceSecurityTest {

    private MailboxItemMapper mailboxMapper;
    private MailAiResultMapper aiResultMapper;
    private InternalToolService service;

    @BeforeEach
    void setUp() {
        var mailMapper = mock(MailMessageMapper.class);
        var recipientMapper = mock(MailRecipientMapper.class);
        mailboxMapper = mock(MailboxItemMapper.class);
        aiResultMapper = mock(MailAiResultMapper.class);
        var categoryService = mock(CategoryService.class);
        service = new InternalToolService(mailMapper, recipientMapper, mailboxMapper, aiResultMapper, categoryService);
    }

    @Test
    void saveAiResultRejectsMailUserPairWithoutVisibleMailboxItem() {
        when(mailboxMapper.findVisibleByUserAndMail(77L, 900L)).thenReturn(null);

        assertThatThrownBy(() -> service.saveAiResult(new SaveAiResultRequest(
                900L,
                77L,
                "ANALYSIS",
                "{\"summary\":[]}",
                "SUCCEEDED"
        ))).isInstanceOf(BusinessException.class);
        verify(aiResultMapper, never()).insert(org.mockito.ArgumentMatchers.any(MailAiResult.class));
    }

    @Test
    void executeActionRejectsSetCategoryWhenCategoryIdIsMissing() {
        MailboxItem item = new MailboxItem();
        item.setId(88L);
        item.setUserId(1L);
        item.setMailId(900L);
        item.setDeletedFlag(false);
        when(mailboxMapper.selectById(88L)).thenReturn(item);

        assertThatThrownBy(() -> service.executeAction(new InternalMailActionRequest(
                1L,
                88L,
                null,
                "SET_CATEGORY",
                null,
                null,
                null,
                null,
                null,
                null
        ))).isInstanceOf(BusinessException.class);
    }
}
