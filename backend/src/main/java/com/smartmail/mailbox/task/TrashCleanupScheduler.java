package com.smartmail.mailbox.task;

import com.smartmail.mailbox.service.MailboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class TrashCleanupScheduler {
    private final MailboxService mailboxService;
    private final long retentionDays;

    public TrashCleanupScheduler(
            MailboxService mailboxService,
            @Value("${smartmail.trash.retention-days:30}") long retentionDays
    ) {
        this.mailboxService = mailboxService;
        this.retentionDays = Math.max(retentionDays, 1);
    }

    @Scheduled(cron = "${smartmail.trash.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpiredTrash() {
        mailboxService.purgeExpiredTrash(LocalDateTime.now().minusDays(retentionDays));
    }
}
