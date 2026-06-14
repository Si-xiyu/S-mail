package com.smartmail.workspace.service;

import com.smartmail.ai.entity.MailAiResult;
import com.smartmail.ai.mapper.MailAiResultMapper;
import com.smartmail.attachment.dto.AttachmentResponse;
import com.smartmail.attachment.entity.MailAttachment;
import com.smartmail.attachment.mapper.MailAttachmentMapper;
import com.smartmail.category.entity.MailCategory;
import com.smartmail.category.entity.MailCategoryAssignment;
import com.smartmail.category.mapper.MailCategoryAssignmentMapper;
import com.smartmail.category.mapper.MailCategoryMapper;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.response.PageResponse;
import com.smartmail.common.security.UserContext;
import com.smartmail.mail.entity.MailMessage;
import com.smartmail.mail.entity.MailRecipient;
import com.smartmail.mail.mapper.MailMessageMapper;
import com.smartmail.mail.mapper.MailRecipientMapper;
import com.smartmail.mailbox.entity.MailboxItem;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import com.smartmail.workspace.dto.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final MailboxItemMapper mailboxItemMapper;
    private final MailMessageMapper mailMessageMapper;
    private final MailRecipientMapper recipientMapper;
    private final MailAiResultMapper aiResultMapper;
    private final MailCategoryMapper categoryMapper;
    private final MailCategoryAssignmentMapper assignmentMapper;
    private final MailAttachmentMapper mailAttachmentMapper;

    public WorkspaceService(
            MailboxItemMapper mailboxItemMapper,
            MailMessageMapper mailMessageMapper,
            MailRecipientMapper recipientMapper,
            MailAiResultMapper aiResultMapper,
            MailCategoryMapper categoryMapper,
            MailCategoryAssignmentMapper assignmentMapper,
            MailAttachmentMapper mailAttachmentMapper
    ) {
        this.mailboxItemMapper = mailboxItemMapper;
        this.mailMessageMapper = mailMessageMapper;
        this.recipientMapper = recipientMapper;
        this.aiResultMapper = aiResultMapper;
        this.categoryMapper = categoryMapper;
        this.assignmentMapper = assignmentMapper;
        this.mailAttachmentMapper = mailAttachmentMapper;
    }

    public WorkspaceViewsResponse getViews() {
        Long userId = UserContext.requireUserId();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        // Smart views
        List<ViewItem> views = List.of(
                new ViewItem("today", "Today", mailboxItemMapper.countToday(userId, startOfDay)),
                new ViewItem("important", "Important", mailboxItemMapper.countImportant(userId)),
                new ViewItem("unread", "Unread", mailboxItemMapper.countUnreadByFolder(userId, "INBOX")),
                new ViewItem("junk", "Junk", mailboxItemMapper.countByFolder(userId, "JUNK"))
        );

        // Folders
        List<FolderItem> folders = List.of(
                new FolderItem("inbox", "Inbox", mailboxItemMapper.countByFolder(userId, "INBOX")),
                new FolderItem("sent", "Sent", mailboxItemMapper.countByFolder(userId, "SENT")),
                new FolderItem("drafts", "Drafts", mailboxItemMapper.countByFolder(userId, "DRAFTS")),
                new FolderItem("trash", "Trash", mailboxItemMapper.countByFolder(userId, "TRASH"))
        );

        // Categories
        List<MailCategory> categories = categoryMapper.listByUser(userId);
        List<CategoryItem> categoryItems = new ArrayList<>();
        for (MailCategory c : categories) {
            long count = assignmentMapper.countByCategory(c.getId(), userId);
            categoryItems.add(new CategoryItem(c.getId(), c.getName(), c.getColor(), count));
        }

        return new WorkspaceViewsResponse(views, folders, categoryItems);
    }

    public PageResponse<MailItemBriefResponse> listMailItems(
            String view, Long categoryId, String keyword, long page, long pageSize) {
        Long userId = UserContext.requireUserId();

        // Determine folder and query strategy
        List<MailboxItem> items;
        long total;
        if ("today".equals(view)) {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            items = mailboxItemMapper.listToday(userId, startOfDay);
            total = items.size();
        } else if ("important".equals(view)) {
            items = mailboxItemMapper.listImportant(userId);
            total = items.size();
        } else if ("unread".equals(view)) {
            items = mailboxItemMapper.listUnread(userId);
            total = items.size();
        } else {
            String folder = mapViewToFolder(view);
            long offset = (page - 1) * pageSize;
            items = mailboxItemMapper.listByFolder(userId, folder, pageSize, offset);
            total = mailboxItemMapper.countByFolder(userId, folder);
        }

        // If category filter, narrow by category assignments
        if (categoryId != null) {
            List<Long> mailIdsInCategory = assignmentMapper.listMailIdsByCategory(categoryId, userId);
            Set<Long> mailIdsSet = new HashSet<>(mailIdsInCategory);
            items = items.stream()
                    .filter(it -> mailIdsSet.contains(it.getMailId()))
                    .collect(Collectors.toList());
            total = items.size();
        }

        // If keyword filter, narrow by keyword in subject/body
        if (keyword != null && !keyword.isBlank()) {
            String lowerKw = keyword.toLowerCase();
            Set<Long> matchingMailIds = new HashSet<>();
            for (MailboxItem it : items) {
                MailMessage msg = mailMessageMapper.selectById(it.getMailId());
                if (msg != null && (
                        (msg.getSubject() != null && msg.getSubject().toLowerCase().contains(lowerKw)) ||
                                (msg.getSenderEmail() != null && msg.getSenderEmail().toLowerCase().contains(lowerKw)) ||
                                (msg.getContentText() != null && msg.getContentText().toLowerCase().contains(lowerKw))
                )) {
                    matchingMailIds.add(it.getMailId());
                }
            }
            items = items.stream()
                    .filter(it -> matchingMailIds.contains(it.getMailId()))
                    .collect(Collectors.toList());
            total = items.size();
        }

        // Apply pagination for non-smart views
        if (!List.of("today", "important", "unread").contains(view)) {
            long offset = (page - 1) * pageSize;
            int from = (int) offset;
            int to = Math.min(from + (int) pageSize, items.size());
            if (from >= items.size()) {
                items = List.of();
            } else {
                items = items.subList(from, to);
            }
        }

        // Assemble responses
        List<MailItemBriefResponse> records = new ArrayList<>();
        Set<Long> mailIds = items.stream().map(MailboxItem::getMailId).collect(Collectors.toSet());
        Map<Long, MailMessage> messageMap = loadMessages(mailIds);
        Map<Long, MailAiResult> latestAiMap = loadLatestAiResults(mailIds, userId);
        Map<Long, MailCategory> categoryMap = loadCategories(mailIds, userId);

        for (MailboxItem item : items) {
            MailMessage msg = messageMap.get(item.getMailId());
            if (msg == null) continue;

            MailAiResult aiResult = latestAiMap.get(item.getMailId());
            String summaryPreview = "";
            String analysisStatus = "PENDING";
            if (aiResult != null) {
                analysisStatus = aiResult.getStatus();
                String json = aiResult.getResultJson();
                if (json != null && json.contains("summary")) {
                    summaryPreview = extractSummaryPreview(json);
                }
            }
            if (summaryPreview.isEmpty()) {
                summaryPreview = msg.getContentText() != null
                        ? (msg.getContentText().length() > 80 ? msg.getContentText().substring(0, 80) + "..." : msg.getContentText())
                        : "";
            }

            MailCategory cat = categoryMap.get(item.getMailId());
            MailItemBriefResponse.CategoryBrief catBrief = cat != null
                    ? new MailItemBriefResponse.CategoryBrief(cat.getId(), cat.getName(), cat.getColor())
                    : new MailItemBriefResponse.CategoryBrief(null, "Other", "#64748b");

            records.add(new MailItemBriefResponse(
                    item.getId(),
                    item.getMailId(),
                    item.getFolder(),
                    msg.getSenderEmail(),
                    msg.getSubject(),
                    summaryPreview,
                    catBrief,
                    analysisStatus,
                    item.getReadFlag(),
                    item.getStarFlag(),
                    item.getPriority(),
                    msg.getHasAttachment(),
                    item.getReceivedAt()
            ));
        }

        return new PageResponse<>(records, total, page, pageSize);
    }

    public MailItemDetailResponse getMailItemDetail(Long itemId) {
        Long userId = UserContext.requireUserId();
        MailboxItem item = mailboxItemMapper.selectById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(404, "邮件不存在或无权访问");
        }

        MailMessage msg = mailMessageMapper.selectById(item.getMailId());
        if (msg == null) {
            throw new BusinessException(404, "邮件原文不存在");
        }

        // Recipients
        List<MailRecipient> recipients = recipientMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MailRecipient>()
                        .eq("mail_id", item.getMailId())
        );
        List<String> recipientEmails = recipients.stream()
                .map(MailRecipient::getRecipientEmail)
                .collect(Collectors.toList());

        // AI results
        List<MailAiResult> aiResults = aiResultMapper.listByMailAndUser(item.getMailId(), userId);
        String analysisStatus = "PENDING";
        List<String> summaryPoints = List.of();
        MailItemBriefResponse.CategoryBrief catBrief = new MailItemBriefResponse.CategoryBrief(null, "Other", "#64748b");
        boolean junk = false;
        List<String> riskHints = List.of();

        if (!aiResults.isEmpty()) {
            analysisStatus = "SUCCEEDED";
            for (MailAiResult r : aiResults) {
                if ("SUMMARY".equals(r.getResultType())) {
                    summaryPoints = extractSummaryPoints(r.getResultJson());
                } else if ("ANALYZE".equals(r.getResultType())) {
                    analysisStatus = "SUCCEEDED".equals(r.getStatus()) ? "SUCCEEDED" : "FAILED";
                }
            }
        }

        // Category
        MailCategoryAssignment assignment = assignmentMapper.findByMailAndUser(item.getMailId(), userId);
        if (assignment != null) {
            MailCategory cat = categoryMapper.selectById(assignment.getCategoryId());
            if (cat != null) {
                catBrief = new MailItemBriefResponse.CategoryBrief(cat.getId(), cat.getName(), cat.getColor());
                junk = "Junk Mail".equals(cat.getName());
            }
        }

        // Attachments
        List<MailAttachment> attachments = mailAttachmentMapper.listByMailId(item.getMailId());
        List<AttachmentResponse> attachmentResponses = attachments.stream()
                .map(a -> new AttachmentResponse(
                        a.getId(), a.getOriginalName(), a.getMimeType(), a.getFileSize(),
                        "/api/v1/attachments/" + a.getId() + "/download"))
                .collect(Collectors.toList());

        return new MailItemDetailResponse(
                item.getId(),
                item.getMailId(),
                item.getFolder(),
                msg.getSenderEmail(),
                recipientEmails,
                msg.getSubject(),
                msg.getContentText(),
                msg.getContentHtml(),
                item.getReadFlag(),
                item.getStarFlag(),
                item.getPriority(),
                new MailItemDetailResponse.AnalysisSummary(analysisStatus, summaryPoints, catBrief, junk, riskHints),
                attachmentResponses,
                new MailItemDetailResponse.AgentAvailability(true),
                msg.getSentAt()
        );
    }

    private String mapViewToFolder(String view) {
        return switch (view) {
            case "sent" -> "SENT";
            case "drafts" -> "DRAFTS";
            case "trash" -> "TRASH";
            case "junk" -> "JUNK";
            default -> "INBOX";
        };
    }

    private Map<Long, MailMessage> loadMessages(Set<Long> mailIds) {
        if (mailIds.isEmpty()) return Map.of();
        List<MailMessage> msgs = mailMessageMapper.selectBatchIds(mailIds);
        Map<Long, MailMessage> map = new HashMap<>();
        for (MailMessage m : msgs) {
            map.put(m.getId(), m);
        }
        return map;
    }

    private Map<Long, MailAiResult> loadLatestAiResults(Set<Long> mailIds, Long userId) {
        Map<Long, MailAiResult> map = new HashMap<>();
        for (Long mailId : mailIds) {
            List<MailAiResult> results = aiResultMapper.listByMailAndUser(mailId, userId);
            if (!results.isEmpty()) {
                map.put(mailId, results.get(0)); // Already ordered by created_at DESC
            }
        }
        return map;
    }

    private Map<Long, MailCategory> loadCategories(Set<Long> mailIds, Long userId) {
        Map<Long, MailCategory> map = new HashMap<>();
        for (Long mailId : mailIds) {
            MailCategoryAssignment assignment = assignmentMapper.findByMailAndUser(mailId, userId);
            if (assignment != null) {
                MailCategory cat = categoryMapper.selectById(assignment.getCategoryId());
                if (cat != null) {
                    map.put(mailId, cat);
                }
            }
        }
        return map;
    }

    private String extractSummaryPreview(String json) {
        if (json == null) return "";
        try {
            // Simple extraction: find "summary" array in JSON-like string
            int idx = json.indexOf("\"summary\"");
            if (idx >= 0) {
                int start = json.indexOf("[", idx);
                int end = json.indexOf("]", start);
                if (start >= 0 && end >= 0) {
                    String arr = json.substring(start + 1, end);
                    // Extract first quoted string
                    int q1 = arr.indexOf("\"");
                    if (q1 >= 0) {
                        int q2 = arr.indexOf("\"", q1 + 1);
                        if (q2 >= 0) {
                            String s = arr.substring(q1 + 1, q2);
                            return s.length() > 80 ? s.substring(0, 80) + "..." : s;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private List<String> extractSummaryPoints(String json) {
        if (json == null) return List.of();
        List<String> points = new ArrayList<>();
        try {
            int idx = json.indexOf("\"summary\"");
            if (idx >= 0) {
                int start = json.indexOf("[", idx);
                int end = json.indexOf("]", start);
                if (start >= 0 && end >= 0) {
                    String arr = json.substring(start + 1, end);
                    String[] parts = arr.split(",");
                    for (String part : parts) {
                        String trimmed = part.trim().replaceAll("^\"|\"$", "");
                        if (!trimmed.isEmpty()) {
                            points.add(trimmed);
                            if (points.size() >= 3) break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return points;
    }
}
