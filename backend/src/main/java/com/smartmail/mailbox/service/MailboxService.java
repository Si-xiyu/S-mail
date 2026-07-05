package com.smartmail.mailbox.service;

import com.smartmail.category.service.CategoryService;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.response.PageResponse;
import com.smartmail.common.security.UserContext;
import com.smartmail.mail.entity.MailMessage;
import com.smartmail.mail.mapper.MailMessageMapper;
import com.smartmail.mailbox.dto.MailboxItemResponse;
import com.smartmail.mailbox.entity.MailboxItem;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class MailboxService {
    private static final Set<String> MOVABLE_FOLDERS = Set.of("INBOX", "JUNK", "TRASH", "SENT");

    private final MailboxItemMapper mailboxMapper;
    private final MailMessageMapper mailMapper;
    private final CategoryService categoryService;

    public MailboxService(MailboxItemMapper mailboxMapper, MailMessageMapper mailMapper,
                          CategoryService categoryService) {
        this.mailboxMapper = mailboxMapper;
        this.mailMapper = mailMapper;
        this.categoryService = categoryService;
    }

    public PageResponse<MailboxItemResponse> list(String folder, long page, long pageSize) {
        Long userId = UserContext.requireUserId();
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(pageSize, 1), 50);

        List<MailboxItemResponse> records;
        long total;

        // 判断是否是自定义分类（数字 ID）还是标准文件夹
        if (isNumeric(folder)) {
            // 按分类查询
            long categoryId = Long.parseLong(folder);
            // 获取该分类下的所有邮件 ID
            List<Long> mailIds = categoryService.getMailIdsByCategory(categoryId, userId);

            if (mailIds.isEmpty()) {
                records = List.of();
                total = 0;
            } else {
                records = mailboxMapper
                        .listByMailIds(userId, mailIds, safeSize, (safePage - 1) * safeSize)
                        .stream()
                        .map(this::toResponse)
                        .toList();
                total = mailboxMapper.countByMailIds(userId, mailIds);
            }
        } else {
            // 按文件夹查询（标准文件夹）
            records = mailboxMapper
                    .listByFolder(userId, normalizeFolder(folder), safeSize, (safePage - 1) * safeSize)
                    .stream()
                    .map(this::toResponse)
                    .toList();
            total = mailboxMapper.countByFolder(userId, normalizeFolder(folder));
        }

        return new PageResponse<>(records, total, safePage, safeSize);
    }

    public void markRead(Long itemId, boolean read) {
        MailboxItem item = requireOwnedItem(itemId);
        item.setReadFlag(read);
        item.setUpdatedAt(LocalDateTime.now());
        mailboxMapper.updateById(item);
    }

    public void star(Long itemId, boolean starred) {
        MailboxItem item = requireOwnedItem(itemId);
        item.setStarFlag(starred);
        item.setUpdatedAt(LocalDateTime.now());
        mailboxMapper.updateById(item);
    }

    public void move(Long itemId, String folder) {
        MailboxItem item = requireOwnedItem(itemId);
        String targetFolder = normalizeFolder(folder);

        // 特殊处理：RESTORE 表示从 TRASH 恢复到原来的位置
        if ("RESTORE".equals(targetFolder)) {
            if (!"TRASH".equals(item.getFolder())) {
                throw new BusinessException(400, "Only items in trash can be restored");
            }
            if (item.getOriginalFolder() != null) {
                item.setFolder(item.getOriginalFolder());
                item.setOriginalFolder(null);
            } else {
                // 如果没有 originalFolder，默认恢复到 INBOX
                item.setFolder("INBOX");
            }
        } else {
            // 普通移动操作
            if (!MOVABLE_FOLDERS.contains(targetFolder)) {
                throw new BusinessException(400, "Unsupported mailbox folder");
            }
            if ("SENT".equals(targetFolder) && !"SENT".equals(item.getFolder())) {
                throw new BusinessException(400, "Only sent items can be restored to Sent");
            }

            // 移动到目标 folder，并清除 originalFolder
            item.setFolder(targetFolder);
            item.setOriginalFolder(null);
        }

        item.setUpdatedAt(LocalDateTime.now());
        mailboxMapper.updateById(item);

        com.smartmail.category.entity.MailCategory junkCat =
                categoryService.findDefaultCategory(UserContext.requireUserId(), CategoryService.DEFAULT_JUNK);
        com.smartmail.category.entity.MailCategory otherCat =
                categoryService.findDefaultCategory(UserContext.requireUserId(), CategoryService.DEFAULT_OTHER);
        if ("JUNK".equals(item.getFolder()) && junkCat != null) {
            categoryService.assignCategory(item.getMailId(), junkCat.getId(), "MANUAL");
        } else if ("INBOX".equals(item.getFolder()) && junkCat != null) {
            com.smartmail.category.entity.MailCategory currentCat =
                    categoryService.getCategoryForMail(item.getMailId(), UserContext.requireUserId());
            if (currentCat != null && CategoryService.DEFAULT_JUNK.equals(currentCat.getName()) && otherCat != null) {
                categoryService.assignCategory(item.getMailId(), otherCat.getId(), "MANUAL");
            }
        }
    }

    public void delete(Long itemId) {
        MailboxItem item = requireOwnedItem(itemId);
        if ("TRASH".equals(item.getFolder())) {
            item.setDeletedFlag(true);
        } else {
            // 保存原始 folder，用于恢复时使用
            item.setOriginalFolder(item.getFolder());
            item.setFolder("TRASH");
        }
        item.setUpdatedAt(LocalDateTime.now());
        mailboxMapper.updateById(item);
    }

    public void changeCategory(Long itemId, Long categoryId) {
        MailboxItem item = requireOwnedItem(itemId);
        categoryService.assignCategory(item.getMailId(), categoryId, "MANUAL");
    }

    private MailboxItemResponse toResponse(MailboxItem item) {
        MailMessage message = mailMapper.selectById(item.getMailId());
        if (message == null) {
            throw new BusinessException(500, "邮件消息不存在: mailId=" + item.getMailId());
        }
        String content = message.getContentText() == null ? "" : message.getContentText().replaceAll("\\s+", " ");
        String preview = content.length() > 80 ? content.substring(0, 80) : content;
        return new MailboxItemResponse(
                item.getId(),
                item.getMailId(),
                message.getSenderEmail(),
                message.getSubject(),
                preview,
                item.getFolder(),
                item.getReadFlag(),
                item.getStarFlag(),
                item.getPriority(),
                message.getHasAttachment(),
                item.getReceivedAt()
        );
    }

    private MailboxItem requireOwnedItem(Long itemId) {
        Long userId = UserContext.requireUserId();
        MailboxItem item = mailboxMapper.selectById(itemId);
        if (item == null || !userId.equals(item.getUserId()) || Boolean.TRUE.equals(item.getDeletedFlag())) {
            throw new BusinessException(404, "邮箱条目不存在");
        }
        return item;
    }

    private String normalizeFolder(String folder) {
        return folder == null || folder.isBlank() ? "INBOX" : folder.trim().toUpperCase();
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        try {
            Long.parseLong(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
