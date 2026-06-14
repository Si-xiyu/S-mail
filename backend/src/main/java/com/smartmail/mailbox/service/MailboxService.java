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
        List<MailboxItemResponse> records = mailboxMapper
                .listByFolder(userId, normalizeFolder(folder), safeSize, (safePage - 1) * safeSize)
                .stream()
                .map(this::toResponse)
                .toList();
        long total = mailboxMapper.countByFolder(userId, normalizeFolder(folder));
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

    public void delete(Long itemId) {
        MailboxItem item = requireOwnedItem(itemId);
        if ("TRASH".equals(item.getFolder())) {
            item.setDeletedFlag(true);
        } else {
            item.setFolder("TRASH");
        }
        item.setUpdatedAt(LocalDateTime.now());
        mailboxMapper.updateById(item);
    }

    private MailboxItemResponse toResponse(MailboxItem item) {
        MailMessage message = mailMapper.selectById(item.getMailId());
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

    public void changeCategory(Long itemId, Long categoryId) {
        MailboxItem item = requireOwnedItem(itemId);
        categoryService.assignCategory(item.getMailId(), categoryId, "MANUAL");
    }

    public void move(Long itemId, String targetFolder) {
        MailboxItem item = requireOwnedItem(itemId);
        String folder = targetFolder.trim().toUpperCase();
        if (!Set.of("JUNK", "INBOX", "TRASH").contains(folder)) {
            throw new BusinessException(400, "不支持的目标文件夹: " + targetFolder);
        }
        item.setFolder(folder);
        item.setUpdatedAt(LocalDateTime.now());
        mailboxMapper.updateById(item);

        // Auto-assign/remove Junk Mail category
        com.smartmail.category.entity.MailCategory junkCat =
                categoryService.findDefaultCategory(UserContext.requireUserId(), CategoryService.DEFAULT_JUNK);
        com.smartmail.category.entity.MailCategory otherCat =
                categoryService.findDefaultCategory(UserContext.requireUserId(), CategoryService.DEFAULT_OTHER);
        if ("JUNK".equals(folder) && junkCat != null) {
            categoryService.assignCategory(item.getMailId(), junkCat.getId(), "MANUAL");
        } else if ("INBOX".equals(folder) && junkCat != null) {
            com.smartmail.category.entity.MailCategory currentCat =
                    categoryService.getCategoryForMail(item.getMailId(), UserContext.requireUserId());
            if (currentCat != null && CategoryService.DEFAULT_JUNK.equals(currentCat.getName()) && otherCat != null) {
                categoryService.assignCategory(item.getMailId(), otherCat.getId(), "MANUAL");
            }
        }
    }

    private String normalizeFolder(String folder) {
        return folder == null || folder.isBlank() ? "INBOX" : folder.trim().toUpperCase();
    }
}
