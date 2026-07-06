package com.smartmail.notification.service;

import com.smartmail.category.entity.MailCategory;
import com.smartmail.category.mapper.MailCategoryMapper;
import com.smartmail.common.security.UserContext;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import com.smartmail.notification.dto.NotificationPollResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificationService {
    private final MailboxItemMapper mailboxMapper;
    private final MailCategoryMapper categoryMapper;

    public NotificationService(MailboxItemMapper mailboxMapper, MailCategoryMapper categoryMapper) {
        this.mailboxMapper = mailboxMapper;
        this.categoryMapper = categoryMapper;
    }

    public NotificationPollResponse poll(LocalDateTime since) {
        Long userId = UserContext.requireUserId();
        LocalDateTime cursor = LocalDateTime.now();
        LocalDateTime effectiveSince = since == null ? cursor.minusDays(1) : since;
        Map<String, Long> unreadByLabel = new LinkedHashMap<>();
        for (String folder : new String[]{"INBOX", "SENT", "TRASH", "JUNK"}) {
            unreadByLabel.put(folder, mailboxMapper.countUnreadByFolder(userId, folder));
        }
        unreadByLabel.put("STARRED", mailboxMapper.countUnreadStarred(userId));
        for (MailCategory category : categoryMapper.listByUser(userId)) {
            unreadByLabel.put(
                    category.getId().toString(),
                    mailboxMapper.countUnreadByCategory(userId, category.getId())
            );
        }
        return new NotificationPollResponse(
                mailboxMapper.countUnread(userId),
                mailboxMapper.countSince(userId, effectiveSince, cursor),
                mailboxMapper.countByFolder(userId, "INBOX"),
                mailboxMapper.countByFolder(userId, "JUNK"),
                unreadByLabel,
                cursor
        );
    }
}
