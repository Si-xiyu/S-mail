package com.smartmail.notification.service;

import com.smartmail.common.security.UserContext;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import com.smartmail.notification.dto.NotificationPollResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationService {
    private final MailboxItemMapper mailboxMapper;

    public NotificationService(MailboxItemMapper mailboxMapper) {
        this.mailboxMapper = mailboxMapper;
    }

    public NotificationPollResponse poll(LocalDateTime since) {
        Long userId = UserContext.requireUserId();
        LocalDateTime effectiveSince = since == null ? LocalDateTime.now().minusDays(1) : since;
        return new NotificationPollResponse(
                mailboxMapper.countUnread(userId),
                mailboxMapper.countSince(userId, effectiveSince),
                mailboxMapper.countByFolder(userId, "INBOX"),
                mailboxMapper.countByFolder(userId, "JUNK")
        );
    }
}
