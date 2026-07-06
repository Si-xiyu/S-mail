package com.smartmail.mail.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartmail.ai.mapper.MailAiResultMapper;
import com.smartmail.analysis.service.AnalysisService;
import com.smartmail.attachment.service.AttachmentService;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.CurrentUser;
import com.smartmail.common.security.UserContext;
import com.smartmail.mail.dto.MailDetailResponse;
import com.smartmail.mail.dto.MailSendResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MailService {
    private final MailMessageMapper mailMapper;
    private final MailRecipientMapper recipientMapper;
    private final MailboxItemMapper mailboxMapper;
    private final SysUserMapper userMapper;
    private final MailAiResultMapper aiResultMapper;
    private final AttachmentService attachmentService;
    private final AnalysisService analysisService;

    public MailService(
            MailMessageMapper mailMapper,
            MailRecipientMapper recipientMapper,
            MailboxItemMapper mailboxMapper,
            SysUserMapper userMapper,
            MailAiResultMapper aiResultMapper,
            AttachmentService attachmentService,
            AnalysisService analysisService
    ) {
        this.mailMapper = mailMapper;
        this.recipientMapper = recipientMapper;
        this.mailboxMapper = mailboxMapper;
        this.userMapper = userMapper;
        this.aiResultMapper = aiResultMapper;
        this.attachmentService = attachmentService;
        this.analysisService = analysisService;
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

        message.setParentMailId(request.parentMailId());
        message.setThreadId(resolveThreadId(request.parentMailId()));

        message.setSentAt(now);
        message.setCreatedAt(now);
        mailMapper.insert(message);

        if (request.pendingAttachmentIds() != null && !request.pendingAttachmentIds().isEmpty()) {
            attachmentService.bindToMail(message.getId(), request.pendingAttachmentIds());
            message.setHasAttachment(true);
            mailMapper.updateById(message);
        }

        createMailboxItem(sender.id(), message.getId(), "SENT", true, now);
        Map<String, String> recipientTypes = new LinkedHashMap<>();
        addRecipients(recipientTypes, request.to(), "TO");
        addRecipients(recipientTypes, request.cc(), "CC");
        addRecipients(recipientTypes, request.bcc(), "BCC");
        List<String> delivered = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (Map.Entry<String, String> recipientEntry : recipientTypes.entrySet()) {
            String email = recipientEntry.getKey();
            SysUser recipientUser = userMapper.findByEmail(email);
            MailRecipient recipient = new MailRecipient();
            recipient.setMailId(message.getId());
            recipient.setRecipientId(recipientUser == null ? null : recipientUser.getId());
            recipient.setRecipientEmail(email);
            recipient.setRecipientType(recipientEntry.getValue());
            recipient.setDeliveryStatus(recipientUser == null ? "FAILED" : "DELIVERED");
            recipient.setCreatedAt(now);
            recipientMapper.insert(recipient);
            if (recipientUser != null) {
                MailboxItem item = createMailboxItem(recipientUser.getId(), message.getId(), "INBOX", false, now);
                analysisService.createTask(item.getId(), message.getId(), recipientUser.getId());
                delivered.add(email);
            } else {
                failed.add(email);
            }
        }
        return new MailSendResponse(message.getId(), message.getMessageNo(), delivered, failed);
    }

    public MailDetailResponse detail(Long mailId) {
        Long userId = UserContext.requireUserId();
        MailboxItem item = mailboxMapper.findVisibleByUserAndMail(userId, mailId);
        if (item == null) {
            throw new BusinessException(404, "邮件不存在或无权访问");
        }
        MailMessage message = mailMapper.selectById(mailId);
        boolean senderView = message.getSenderId().equals(userId);
        List<String> recipients = recipientMapper.selectList(
                new QueryWrapper<MailRecipient>().eq("mail_id", mailId)
        ).stream()
                .filter(recipient -> senderView || !"BCC".equals(recipient.getRecipientType()))
                .map(MailRecipient::getRecipientEmail)
                .toList();
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
                aiResults,
                attachmentService.getAttachmentsForMail(mailId)
        );
    }

    private MailboxItem createMailboxItem(Long userId, Long mailId, String folder, boolean read, LocalDateTime now) {
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
        return item;
    }

    /**
     * 获取邮件所属的线程
     */
    @Transactional(readOnly = true)
    public List<ThreadMessageResponse> getThread(Long mailId) {
        Long userId = UserContext.requireUserId();
        MailMessage requested = requireVisibleMessage(mailId, userId);
        Long rootId = requested.getThreadId() != null ? requested.getThreadId() : requested.getId();

        List<MailMessage> thread = new ArrayList<>(mailMapper.findByThreadId(rootId));
        MailMessage root = mailMapper.selectById(rootId);
        if (root != null && thread.stream().noneMatch(message -> message.getId().equals(rootId))) {
            thread.add(root);
        }

        return thread.stream()
                .filter(message -> mailboxMapper.findVisibleByUserAndMail(userId, message.getId()) != null)
                .sorted((left, right) -> left.getSentAt().compareTo(right.getSentAt()))
                .map(this::toThreadResponse)
                .toList();
    }

    /**
     * 获取邮件的对话路径（从第一封到当前邮件）
     *
     * 功能：用于前端展示"对话链"UI
     * 通过回溯 parentMailId 获取从链起点到当前邮件的所有邮件
     */
    @Transactional(readOnly = true)
    public List<ThreadMessageResponse> getMailPath(Long mailId) {
        Long userId = UserContext.requireUserId();
        List<ThreadMessageResponse> path = new ArrayList<>();
        MailMessage current = requireVisibleMessage(mailId, userId);
        Set<Long> visited = new HashSet<>();
        int maxDepth = 1000;

        while (current != null) {
            if (!visited.add(current.getId()) || visited.size() > maxDepth) {
                throw new BusinessException(500, "邮件链深度超限");
            }
            path.add(toThreadResponse(current));

            if (current.getParentMailId() == null) {
                break;
            }
            current = requireVisibleMessage(current.getParentMailId(), userId);
        }

        Collections.reverse(path);
        return path;
    }

    private Long resolveThreadId(Long parentMailId) {
        if (parentMailId == null) {
            return null;
        }
        Long userId = UserContext.requireUserId();
        MailMessage parentMail = requireVisibleMessage(parentMailId, userId);
        Long threadId = parentMail.getThreadId() != null ? parentMail.getThreadId() : parentMail.getId();
        if (parentMail.getThreadId() == null) {
            parentMail.setThreadId(threadId);
            mailMapper.updateById(parentMail);
        }
        return threadId;
    }

    private MailMessage requireVisibleMessage(Long mailId, Long userId) {
        if (mailboxMapper.findVisibleByUserAndMail(userId, mailId) == null) {
            throw new BusinessException(404, "邮件不存在或无权访问");
        }
        MailMessage message = mailMapper.selectById(mailId);
        if (message == null) {
            throw new BusinessException(404, "邮件不存在或无权访问");
        }
        return message;
    }

    private ThreadMessageResponse toThreadResponse(MailMessage message) {
        return new ThreadMessageResponse(
                message.getId(),
                message.getSenderEmail(),
                message.getSubject(),
                message.getContentText(),
                message.getSentAt(),
                message.getParentMailId(),
                message.getThreadId()
        );
    }

    private void addRecipients(Map<String, String> recipients, List<String> emails, String type) {
        if (emails == null) {
            return;
        }
        for (String rawEmail : emails) {
            if (rawEmail == null || rawEmail.isBlank()) {
                continue;
            }
            recipients.putIfAbsent(rawEmail.trim().toLowerCase(), type);
        }
    }
}
