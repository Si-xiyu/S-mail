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

        // 处理线程关系
        message.setParentMailId(request.parentMailId());
        Long threadId = request.threadId() != null ? request.threadId() : resolveThreadId(request.subject(), request.parentMailId());
        message.setThreadId(threadId);

        message.setSentAt(now);
        message.setCreatedAt(now);
        mailMapper.insert(message);

        if (request.pendingAttachmentIds() != null && !request.pendingAttachmentIds().isEmpty()) {
            attachmentService.bindToMail(message.getId(), request.pendingAttachmentIds());
            message.setHasAttachment(true);
            mailMapper.updateById(message);
        }

        createMailboxItem(sender.id(), message.getId(), "SENT", true, now);
        List<String> allRecipients = new ArrayList<>();
        allRecipients.addAll(request.to());
        if (request.cc() != null) {
            allRecipients.addAll(request.cc());
        }
        List<String> delivered = new ArrayList<>();
        List<String> failed = new ArrayList<>();
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
    public List<MailMessage> getThread(Long mailId) {
        MailMessage mail = mailMapper.selectById(mailId);
        if (mail == null) {
            throw new BusinessException(404, "邮件不存在");
        }

        List<MailMessage> thread = new ArrayList<>();

        // 如果邮件有 threadId，获取该线程的所有邮件
        if (mail.getThreadId() != null) {
            thread = mailMapper.findByThreadId(mail.getThreadId());
        } else if (mail.getParentMailId() != null) {
            // 如果邮件有 parentMailId，向上回溯找到线程起点
            MailMessage current = mail;
            while (current.getParentMailId() != null) {
                MailMessage parent = mailMapper.selectById(current.getParentMailId());
                if (parent == null) break;
                current = parent;
            }

            // 如果找到了线程起点且它有 threadId，获取整个线程
            if (current.getThreadId() != null) {
                thread = mailMapper.findByThreadId(current.getThreadId());
            } else {
                // 否则逐个查找该邮件的所有回复
                thread.add(current);
                addReplies(current.getId(), thread);
            }
        } else {
            // 如果没有 threadId 或 parentMailId，可能是新线程的起点
            thread.add(mail);
            addReplies(mail.getId(), thread);
        }

        // 按发送时间排序
        thread.sort((a, b) -> a.getSentAt().compareTo(b.getSentAt()));
        return thread;
    }

    /**
     * 获取邮件的对话路径（从第一封到当前邮件）
     *
     * 功能：用于前端展示"对话链"UI
     * 通过回溯 parentMailId 获取从链起点到当前邮件的所有邮件
     */
    @Transactional(readOnly = true)
    public List<MailMessage> getMailPath(Long mailId) {
        MailMessage mail = mailMapper.selectById(mailId);
        if (mail == null) {
            throw new BusinessException(404, "邮件不存在");
        }

        List<MailMessage> path = new ArrayList<>();
        MailMessage current = mail;
        int maxDepth = 1000;
        int depth = 0;

        // 从当前邮件向上回溯到链的起点
        while (current != null) {
            path.add(current);
            depth++;

            if (depth > maxDepth) {
                throw new BusinessException(500, "邮件链深度超限");
            }

            // 如果没有父邮件，说明到达了链的起点
            if (current.getParentMailId() == null) {
                break;
            }

            // 向上一级
            current = mailMapper.selectById(current.getParentMailId());
        }

        // 反转为时间正序：[第一封, ..., 当前]
        Collections.reverse(path);

        return path;
    }

    /**
     * 递归获取邮件的所有回复，添加到 thread 列表
     */
    private void addReplies(Long parentMailId, List<MailMessage> thread) {
        List<MailMessage> replies = mailMapper.findByParentMailId(parentMailId);
        for (MailMessage reply : replies) {
            thread.add(reply);
            addReplies(reply.getId(), thread);
        }
    }

    /**
     * 发送邮件时处理线程
     */
    public Long resolveThreadId(String subject, Long parentMailId) {
        // 如果有 parentMailId，使用其 threadId 或创建新线程
        if (parentMailId != null) {
            MailMessage parentMail = mailMapper.selectById(parentMailId);
            if (parentMail != null) {
                if (parentMail.getThreadId() != null) {
                    return parentMail.getThreadId();
                } else {
                    // 创建新线程，使用 parentMailId 作为线程起点
                    return parentMailId;
                }
            }
        }

        // 如果没有 parentMailId，尝试根据主题查找现有线程
        // 这里简化处理：不返回 threadId，让邮件成为新的线程起点
        return null;
    }
}
