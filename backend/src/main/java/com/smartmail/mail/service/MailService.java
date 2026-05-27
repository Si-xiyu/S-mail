package com.smartmail.mail.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartmail.ai.mapper.MailAiResultMapper;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.CurrentUser;
import com.smartmail.common.security.UserContext;
import com.smartmail.mail.dto.MailDetailResponse;
import com.smartmail.mail.dto.MailSendResponse;
import com.smartmail.mail.dto.SendMailRequest;
import com.smartmail.mail.entity.MailMessage;
import com.smartmail.mail.entity.MailRecipient;
import com.smartmail.mail.mapper.MailMessageMapper;
import com.smartmail.mail.mapper.MailRecipientMapper;
import com.smartmail.mailbox.entity.MailboxItem;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import com.smartmail.user.entity.SysUser;
import com.smartmail.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MailService {
    private final MailMessageMapper mailMapper;
    private final MailRecipientMapper recipientMapper;
    private final MailboxItemMapper mailboxMapper;
    private final SysUserMapper userMapper;
    private final MailAiResultMapper aiResultMapper;

    public MailService(
            MailMessageMapper mailMapper,
            MailRecipientMapper recipientMapper,
            MailboxItemMapper mailboxMapper,
            SysUserMapper userMapper,
            MailAiResultMapper aiResultMapper
    ) {
        this.mailMapper = mailMapper;
        this.recipientMapper = recipientMapper;
        this.mailboxMapper = mailboxMapper;
        this.userMapper = userMapper;
        this.aiResultMapper = aiResultMapper;
    }

    @Transactional
    public MailSendResponse send(SendMailRequest request) {
        CurrentUser sender = UserContext.get();
        if (sender == null) {
            throw new BusinessException(401, "请先登录");
        }
        LocalDateTime now = LocalDateTime.now();
        MailMessage message = new MailMessage();
        message.setMessageNo("SM-" + UUID.randomUUID());
        message.setSenderId(sender.id());
        message.setSenderEmail(sender.email());
        message.setSubject(request.subject().trim());
        message.setContentText(request.contentText());
        message.setContentHtml(request.contentHtml());
        message.setHasAttachment(false);
        message.setSentAt(now);
        message.setCreatedAt(now);
        mailMapper.insert(message);

        createMailboxItem(sender.id(), message.getId(), "SENT", true, now);
        List<String> allRecipients = new ArrayList<>();
        allRecipients.addAll(request.to());
        if (request.cc() != null) {
            allRecipients.addAll(request.cc());
        }
        for (String rawEmail : allRecipients) {
            String email = rawEmail.trim().toLowerCase();
            SysUser recipientUser = userMapper.findByEmail(email);
            MailRecipient recipient = new MailRecipient();
            recipient.setMailId(message.getId());
            recipient.setRecipientId(recipientUser == null ? null : recipientUser.getId());
            recipient.setRecipientEmail(email);
            recipient.setRecipientType(request.to().contains(rawEmail) ? "TO" : "CC");
            recipient.setDeliveryStatus(recipientUser == null ? "FAILED" : "DELIVERED");
            recipient.setCreatedAt(now);
            recipientMapper.insert(recipient);
            if (recipientUser != null) {
                createMailboxItem(recipientUser.getId(), message.getId(), "INBOX", false, now);
            }
        }
        return new MailSendResponse(message.getId(), message.getMessageNo());
    }

    public MailDetailResponse detail(Long mailId) {
        Long userId = UserContext.requireUserId();
        MailboxItem item = mailboxMapper.findVisibleByUserAndMail(userId, mailId);
        if (item == null) {
            throw new BusinessException(404, "邮件不存在或无权访问");
        }
        MailMessage message = mailMapper.selectById(mailId);
        List<String> recipients = recipientMapper.selectList(
                new QueryWrapper<MailRecipient>().eq("mail_id", mailId)
        ).stream().map(MailRecipient::getRecipientEmail).toList();
        List<Map<String, Object>> aiResults = aiResultMapper.listByMailAndUser(mailId, userId)
                .stream()
                .map(result -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("type", result.getResultType());
                    row.put("status", result.getStatus());
                    row.put("resultJson", result.getResultJson());
                    row.put("createdAt", result.getCreatedAt());
                    return row;
                })
                .toList();
        return new MailDetailResponse(
                message.getId(),
                item.getId(),
                message.getMessageNo(),
                message.getSenderEmail(),
                message.getSubject(),
                message.getContentText(),
                message.getContentHtml(),
                item.getFolder(),
                item.getReadFlag(),
                item.getStarFlag(),
                item.getPriority(),
                message.getSentAt(),
                recipients,
                aiResults
        );
    }

    private void createMailboxItem(Long userId, Long mailId, String folder, boolean read, LocalDateTime now) {
        MailboxItem item = new MailboxItem();
        item.setUserId(userId);
        item.setMailId(mailId);
        item.setFolder(folder);
        item.setReadFlag(read);
        item.setStarFlag(false);
        item.setDeletedFlag(false);
        item.setPriority("NORMAL");
        item.setReceivedAt(now);
        item.setUpdatedAt(now);
        mailboxMapper.insert(item);
    }
}
