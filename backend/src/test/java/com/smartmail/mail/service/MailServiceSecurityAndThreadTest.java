package com.smartmail.mail.service;

import com.smartmail.ai.mapper.MailAiResultMapper;
import com.smartmail.analysis.service.AnalysisService;
import com.smartmail.attachment.service.AttachmentService;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.CurrentUser;
import com.smartmail.common.security.UserContext;
import com.smartmail.mail.dto.SendMailRequest;
import com.smartmail.mail.dto.ThreadMessageResponse;
import com.smartmail.mail.entity.MailMessage;
import com.smartmail.mail.entity.MailRecipient;
import com.smartmail.mail.mapper.MailMessageMapper;
import com.smartmail.mail.mapper.MailRecipientMapper;
import com.smartmail.mailbox.entity.MailboxItem;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import com.smartmail.user.entity.SysUser;
import com.smartmail.user.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailServiceSecurityAndThreadTest {

    private MailMessageMapper mailMapper;
    private MailRecipientMapper recipientMapper;
    private MailboxItemMapper mailboxMapper;
    private SysUserMapper userMapper;
    private MailAiResultMapper aiResultMapper;
    private AttachmentService attachmentService;
    private AnalysisService analysisService;
    private MailService service;

    @BeforeEach
    void setUp() {
        mailMapper = mock(MailMessageMapper.class);
        recipientMapper = mock(MailRecipientMapper.class);
        mailboxMapper = mock(MailboxItemMapper.class);
        userMapper = mock(SysUserMapper.class);
        aiResultMapper = mock(MailAiResultMapper.class);
        attachmentService = mock(AttachmentService.class);
        analysisService = mock(AnalysisService.class);
        service = new MailService(mailMapper, recipientMapper, mailboxMapper, userMapper,
                aiResultMapper, attachmentService, analysisService);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void threadAndPathRejectAnonymousAndCrossUserAccess() {
        UserContext.clear();
        assertThatThrownBy(() -> service.getThread(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(401);
        assertThatThrownBy(() -> service.getMailPath(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(401);

        UserContext.set(new CurrentUser(2L, "other@smartmail.local", "Other"));
        when(mailboxMapper.findVisibleByUserAndMail(2L, 10L)).thenReturn(null);
        assertThatThrownBy(() -> service.getThread(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(404);
        assertThatThrownBy(() -> service.getMailPath(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(404);
    }

    @Test
    void threadAndPathReturnVisibleRootAndReplyInOrderAsDto() {
        UserContext.set(new CurrentUser(1L, "user@smartmail.local", "User"));
        LocalDateTime rootTime = LocalDateTime.of(2026, 7, 6, 10, 0);
        MailMessage root = message(10L, null, 10L, rootTime, "Root");
        MailMessage reply = message(11L, 10L, 10L, rootTime.plusMinutes(5), "Reply");
        when(mailboxMapper.findVisibleByUserAndMail(1L, 10L)).thenReturn(item(101L, 10L, 1L));
        when(mailboxMapper.findVisibleByUserAndMail(1L, 11L)).thenReturn(item(102L, 11L, 1L));
        when(mailMapper.selectById(10L)).thenReturn(root);
        when(mailMapper.selectById(11L)).thenReturn(reply);
        when(mailMapper.findByThreadId(10L)).thenReturn(List.of(reply));

        List<ThreadMessageResponse> thread = service.getThread(11L);
        List<ThreadMessageResponse> path = service.getMailPath(11L);

        assertThat(thread).extracting(ThreadMessageResponse::mailId).containsExactly(10L, 11L);
        assertThat(path).extracting(ThreadMessageResponse::mailId).containsExactly(10L, 11L);
        assertThat(thread.get(0).subject()).isEqualTo("Root");
        assertThat(thread.get(1).parentMailId()).isEqualTo(10L);
        assertThat(thread.get(1).sentAt()).isEqualTo(rootTime.plusMinutes(5));
    }

    @Test
    void sendRejectsForgedParentBeforePersistingMail() {
        UserContext.set(new CurrentUser(1L, "sender@smartmail.local", "Sender"));
        when(mailboxMapper.findVisibleByUserAndMail(1L, 999L)).thenReturn(null);

        assertThatThrownBy(() -> service.send(request(List.of("to@smartmail.local"), List.of(), List.of(), 999L)))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(404);
        verify(mailMapper, never()).insert(any(MailMessage.class));
    }

    @Test
    void sendDeduplicatesRecipientsAndPersistsBccType() {
        UserContext.set(new CurrentUser(1L, "sender@smartmail.local", "Sender"));
        when(userMapper.findByEmail("alice@smartmail.local")).thenReturn(user(2L, "alice@smartmail.local"));
        when(userMapper.findByEmail("bob@smartmail.local")).thenReturn(user(3L, "bob@smartmail.local"));
        doAnswer(invocation -> {
            invocation.<MailMessage>getArgument(0).setId(500L);
            return 1;
        }).when(mailMapper).insert(any(MailMessage.class));

        var response = service.send(request(
                List.of("Alice@smartmail.local"),
                List.of("alice@smartmail.local"),
                List.of("bob@smartmail.local", "BOB@smartmail.local"),
                null));

        ArgumentCaptor<MailRecipient> captor = ArgumentCaptor.forClass(MailRecipient.class);
        verify(recipientMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(MailRecipient::getRecipientEmail, MailRecipient::getRecipientType)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("alice@smartmail.local", "TO"),
                        org.assertj.core.groups.Tuple.tuple("bob@smartmail.local", "BCC"));
        assertThat(response.delivery().delivered())
                .containsExactlyInAnyOrder("alice@smartmail.local", "bob@smartmail.local");
    }

    @Test
    void recipientDetailDoesNotExposeBccButSenderDetailDoes() {
        MailMessage mail = message(500L, null, null, LocalDateTime.now(), "Subject");
        mail.setSenderId(1L);
        MailboxItem senderItem = item(100L, 500L, 1L);
        MailboxItem recipientItem = item(200L, 500L, 2L);
        MailRecipient to = recipient(500L, "alice@smartmail.local", "TO");
        MailRecipient bcc = recipient(500L, "bob@smartmail.local", "BCC");
        when(mailMapper.selectById(500L)).thenReturn(mail);
        when(recipientMapper.selectList(any())).thenReturn(List.of(to, bcc));
        when(aiResultMapper.listByMailAndUser(anyLong(), anyLong())).thenReturn(List.of());
        when(attachmentService.getAttachmentsForMail(500L)).thenReturn(List.of());

        UserContext.set(new CurrentUser(2L, "alice@smartmail.local", "Alice"));
        when(mailboxMapper.findVisibleByUserAndMail(2L, 500L)).thenReturn(recipientItem);
        assertThat(service.detail(500L).recipients()).containsExactly("alice@smartmail.local");

        UserContext.set(new CurrentUser(1L, "sender@smartmail.local", "Sender"));
        when(mailboxMapper.findVisibleByUserAndMail(1L, 500L)).thenReturn(senderItem);
        assertThat(service.detail(500L).recipients())
                .containsExactly("alice@smartmail.local", "bob@smartmail.local");
    }

    private SendMailRequest request(List<String> to, List<String> cc, List<String> bcc, Long parentMailId) {
        return new SendMailRequest(to, cc, bcc, "Subject", "Body", null, List.of(), parentMailId);
    }

    private MailMessage message(Long id, Long parentId, Long threadId, LocalDateTime sentAt, String subject) {
        MailMessage message = new MailMessage();
        message.setId(id);
        message.setSenderId(1L);
        message.setSenderEmail("sender@smartmail.local");
        message.setSubject(subject);
        message.setContentText("Body");
        message.setParentMailId(parentId);
        message.setThreadId(threadId);
        message.setSentAt(sentAt);
        return message;
    }

    private MailboxItem item(Long id, Long mailId, Long userId) {
        MailboxItem item = new MailboxItem();
        item.setId(id);
        item.setMailId(mailId);
        item.setUserId(userId);
        item.setFolder("INBOX");
        item.setReadFlag(false);
        item.setStarFlag(false);
        item.setDeletedFlag(false);
        item.setPriority("NORMAL");
        return item;
    }

    private SysUser user(Long id, String email) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setEmail(email);
        user.setStatus("ACTIVE");
        return user;
    }

    private MailRecipient recipient(Long mailId, String email, String type) {
        MailRecipient recipient = new MailRecipient();
        recipient.setMailId(mailId);
        recipient.setRecipientEmail(email);
        recipient.setRecipientType(type);
        return recipient;
    }
}
