package com.smartmail.internal.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartmail.ai.entity.MailAiResult;
import com.smartmail.ai.mapper.MailAiResultMapper;
import com.smartmail.category.service.CategoryService;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.internal.dto.InternalMailResponse;
import com.smartmail.internal.dto.InternalMailActionRequest;
import com.smartmail.internal.dto.InternalMailSearchResponse;
import com.smartmail.internal.dto.SaveAiResultRequest;
import com.smartmail.internal.dto.SetPriorityRequest;
import com.smartmail.mail.entity.MailMessage;
import com.smartmail.mail.entity.MailRecipient;
import com.smartmail.mail.mapper.MailMessageMapper;
import com.smartmail.mail.mapper.MailRecipientMapper;
import com.smartmail.mailbox.entity.MailboxItem;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class InternalToolService {
    private static final Set<String> VALID_MOVE_TARGETS = Set.of("INBOX", "JUNK", "TRASH");

    private final MailMessageMapper mailMapper;
    private final MailRecipientMapper recipientMapper;
    private final MailboxItemMapper mailboxMapper;
    private final MailAiResultMapper aiResultMapper;
    private final CategoryService categoryService;

    public InternalToolService(
            MailMessageMapper mailMapper,
            MailRecipientMapper recipientMapper,
            MailboxItemMapper mailboxMapper,
            MailAiResultMapper aiResultMapper,
            CategoryService categoryService
    ) {
        this.mailMapper = mailMapper;
        this.recipientMapper = recipientMapper;
        this.mailboxMapper = mailboxMapper;
        this.aiResultMapper = aiResultMapper;
        this.categoryService = categoryService;
    }

    public InternalMailResponse getMail(Long mailId, Long userId) {
        MailboxItem item = mailboxMapper.findVisibleByUserAndMail(userId, mailId);
        if (item == null) {
            throw new BusinessException(404, "邮件不存在或用户无权访问");
        }
        MailMessage mail = mailMapper.selectById(mailId);
        List<String> recipients = recipientMapper.selectList(new QueryWrapper<MailRecipient>().eq("mail_id", mailId))
                .stream()
                .map(MailRecipient::getRecipientEmail)
                .toList();
        return new InternalMailResponse(
                item.getId(),
                mailId,
                userId,
                item.getFolder(),
                mail.getSenderEmail(),
                mail.getSubject(),
                mail.getContentText(),
                mail.getContentHtml(),
                item.getPriority(),
                recipients
        );
    }

    public InternalMailResponse getMailContext(Long itemId, Long userId) {
        MailboxItem item = requireOwnedItem(userId, itemId);
        MailMessage mail = mailMapper.selectById(item.getMailId());
        List<String> recipients = recipientMapper.selectList(new QueryWrapper<MailRecipient>().eq("mail_id", mail.getId()))
                .stream()
                .map(MailRecipient::getRecipientEmail)
                .toList();
        return new InternalMailResponse(
                item.getId(),
                mail.getId(),
                userId,
                item.getFolder(),
                mail.getSenderEmail(),
                mail.getSubject(),
                mail.getContentText(),
                mail.getContentHtml(),
                item.getPriority(),
                recipients
        );
    }

    public List<InternalMailSearchResponse> search(Long userId, String keyword, String folder, long limit) {
        String normalizedFolder = folder == null || folder.isBlank() ? null : folder.trim().toUpperCase(Locale.ROOT);
        String needle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        long safeLimit = Math.min(Math.max(limit, 1), 20);
        return mailboxMapper.listVisibleByUser(userId).stream()
                .filter(item -> normalizedFolder == null || normalizedFolder.equals(item.getFolder()))
                .filter(item -> matchesKeyword(item, needle))
                .limit(safeLimit)
                .map(this::toSearchResponse)
                .toList();
    }

    public void saveAiResult(SaveAiResultRequest request) {
        MailAiResult result = new MailAiResult();
        result.setMailId(request.mailId());
        result.setUserId(request.userId());
        result.setResultType(request.resultType());
        result.setResultJson(request.resultJson());
        result.setStatus(request.status());
        result.setCreatedAt(LocalDateTime.now());
        result.setUpdatedAt(LocalDateTime.now());
        aiResultMapper.insert(result);
    }

    public void executeAction(InternalMailActionRequest request) {
        if (request.userId() == null || request.resolvedItemId() == null || request.resolvedAction() == null) {
            throw new BusinessException(400, "userId, mailItemId and action are required");
        }
        MailboxItem item = requireOwnedItem(request.userId(), request.resolvedItemId());
        String action = request.resolvedAction().trim().toUpperCase(Locale.ROOT);
        switch (action) {
            case "MARK_READ" -> item.setReadFlag(true);
            case "MARK_UNREAD" -> item.setReadFlag(false);
            case "SET_READ" -> item.setReadFlag(Boolean.TRUE.equals(request.read()));
            case "STAR" -> item.setStarFlag(true);
            case "UNSTAR" -> item.setStarFlag(false);
            case "MOVE_TO_JUNK" -> item.setFolder("JUNK");
            case "MOVE" -> {
                String target = request.folder() != null ? request.folder() : request.value();
                target = target == null ? "" : target.trim().toUpperCase(Locale.ROOT);
                if (!VALID_MOVE_TARGETS.contains(target)) {
                    throw new BusinessException(400, "Unsupported move target");
                }
                item.setFolder(target);
            }
            case "SET_PRIORITY" -> {
                String priority = request.priority() != null ? request.priority() : request.value();
                priority = priority == null ? "" : priority.trim().toUpperCase(Locale.ROOT);
                if (!Set.of("LOW", "NORMAL", "HIGH", "URGENT").contains(priority)) {
                    throw new BusinessException(400, "Unsupported priority");
                }
                item.setPriority(priority);
            }
            case "SET_CATEGORY" -> {
                if (request.categoryId() == null) {
                    throw new BusinessException(400, "categoryId is required");
                }
                categoryService.assignCategoryForUser(item.getMailId(), request.userId(), request.categoryId(), "AGENT");
            }
            default -> throw new BusinessException(400, "Unsupported mail action");
        }
        item.setUpdatedAt(LocalDateTime.now());
        mailboxMapper.updateById(item);
    }

    public void setPriority(Long mailId, SetPriorityRequest request) {
        MailboxItem item = mailboxMapper.findVisibleByUserAndMail(request.userId(), mailId);
        if (item == null) {
            throw new BusinessException(404, "邮箱条目不存在");
        }
        item.setPriority(request.priority().trim().toUpperCase());
        item.setUpdatedAt(LocalDateTime.now());
        mailboxMapper.updateById(item);
    }

    private MailboxItem requireOwnedItem(Long userId, Long itemId) {
        MailboxItem item = mailboxMapper.selectById(itemId);
        if (item == null || !userId.equals(item.getUserId()) || Boolean.TRUE.equals(item.getDeletedFlag())) {
            throw new BusinessException(404, "Mailbox item not found");
        }
        return item;
    }

    private boolean matchesKeyword(MailboxItem item, String needle) {
        if (needle == null || needle.isBlank()) {
            return true;
        }
        MailMessage mail = mailMapper.selectById(item.getMailId());
        return contains(mail.getSenderEmail(), needle)
                || contains(mail.getSubject(), needle)
                || contains(mail.getContentText(), needle)
                || contains(mail.getContentHtml(), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private InternalMailSearchResponse toSearchResponse(MailboxItem item) {
        MailMessage mail = mailMapper.selectById(item.getMailId());
        String text = mail.getContentText() == null ? "" : mail.getContentText().replaceAll("\\s+", " ").trim();
        String preview = text.length() > 120 ? text.substring(0, 120) : text;
        return new InternalMailSearchResponse(
                item.getId(),
                mail.getId(),
                item.getFolder(),
                mail.getSenderEmail(),
                mail.getSubject(),
                preview,
                item.getPriority(),
                item.getReadFlag(),
                item.getStarFlag(),
                item.getReceivedAt()
        );
    }
}
