package com.smartmail.workspace.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartmail.ai.entity.MailAiResult;
import com.smartmail.ai.mapper.MailAiResultMapper;
import com.smartmail.ai.service.AnalysisTaskService;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.response.PageResponse;
import com.smartmail.common.security.UserContext;
import com.smartmail.mail.entity.MailMessage;
import com.smartmail.mail.entity.MailRecipient;
import com.smartmail.mail.mapper.MailMessageMapper;
import com.smartmail.mail.mapper.MailRecipientMapper;
import com.smartmail.mailbox.entity.MailboxItem;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import com.smartmail.workspace.dto.WorkspaceAgentResponse;
import com.smartmail.workspace.dto.WorkspaceAnalysisResponse;
import com.smartmail.workspace.dto.WorkspaceAttachmentResponse;
import com.smartmail.workspace.dto.WorkspaceCategoryResponse;
import com.smartmail.workspace.dto.WorkspaceCountResponse;
import com.smartmail.workspace.dto.WorkspaceMailDetailResponse;
import com.smartmail.workspace.dto.WorkspaceMailItemResponse;
import com.smartmail.workspace.dto.WorkspaceViewsResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class WorkspaceService {
    private final MailboxItemMapper mailboxMapper;
    private final MailMessageMapper mailMapper;
    private final MailRecipientMapper recipientMapper;
    private final MailAiResultMapper aiResultMapper;
    private final AnalysisTaskService analysisTaskService;

    public WorkspaceService(
            MailboxItemMapper mailboxMapper,
            MailMessageMapper mailMapper,
            MailRecipientMapper recipientMapper,
            MailAiResultMapper aiResultMapper,
            AnalysisTaskService analysisTaskService
    ) {
        this.mailboxMapper = mailboxMapper;
        this.mailMapper = mailMapper;
        this.recipientMapper = recipientMapper;
        this.aiResultMapper = aiResultMapper;
        this.analysisTaskService = analysisTaskService;
    }

    public WorkspaceViewsResponse views() {
        Long userId = UserContext.requireUserId();
        List<WorkspaceCountResponse> views = List.of(
                new WorkspaceCountResponse("today", "Today", mailboxMapper.countToday(userId, LocalDate.now().atStartOfDay())),
                new WorkspaceCountResponse("important", "Important", mailboxMapper.countImportant(userId)),
                new WorkspaceCountResponse("unread", "Unread", mailboxMapper.countUnread(userId)),
                new WorkspaceCountResponse("junk", "Junk", mailboxMapper.countByFolder(userId, "JUNK"))
        );
        List<WorkspaceCountResponse> folders = List.of(
                new WorkspaceCountResponse("inbox", "Inbox", mailboxMapper.countByFolder(userId, "INBOX")),
                new WorkspaceCountResponse("sent", "Sent", mailboxMapper.countByFolder(userId, "SENT")),
                new WorkspaceCountResponse("drafts", "Drafts", 0),
                new WorkspaceCountResponse("trash", "Trash", mailboxMapper.countByFolder(userId, "TRASH"))
        );
        return new WorkspaceViewsResponse(views, folders, List.of());
    }

    public PageResponse<WorkspaceMailItemResponse> list(String view, Long categoryId, String keyword, long page, long pageSize) {
        Long userId = UserContext.requireUserId();
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(pageSize, 1), 50);
        List<MailboxItem> candidates = mailboxMapper.listVisibleByUser(userId);
        List<MailboxItem> filtered = candidates.stream()
                .filter(item -> matchesView(item, normalizeView(view)))
                .filter(item -> categoryId == null)
                .filter(item -> matchesKeyword(item, keyword))
                .sorted(Comparator.comparing(MailboxItem::getReceivedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        int from = (int) Math.min((safePage - 1) * safeSize, filtered.size());
        int to = (int) Math.min(from + safeSize, filtered.size());
        List<WorkspaceMailItemResponse> records = filtered.subList(from, to).stream()
                .map(this::toItemResponse)
                .toList();
        return new PageResponse<>(records, filtered.size(), safePage, safeSize);
    }

    public WorkspaceMailDetailResponse detail(Long itemId) {
        Long userId = UserContext.requireUserId();
        MailboxItem item = requireOwnedItem(userId, itemId);
        MailMessage mail = requireMail(item.getMailId());
        List<String> recipients = recipientMapper.selectList(new QueryWrapper<MailRecipient>().eq("mail_id", mail.getId()))
                .stream()
                .map(MailRecipient::getRecipientEmail)
                .toList();
        return new WorkspaceMailDetailResponse(
                item.getId(),
                mail.getId(),
                item.getFolder(),
                mail.getSenderEmail(),
                recipients,
                mail.getSubject(),
                mail.getContentText(),
                mail.getContentHtml(),
                item.getReadFlag(),
                item.getStarFlag(),
                item.getPriority(),
                analysis(userId, item),
                List.of(),
                new WorkspaceAgentResponse(true)
        );
    }

    private WorkspaceMailItemResponse toItemResponse(MailboxItem item) {
        MailMessage mail = requireMail(item.getMailId());
        WorkspaceAnalysisResponse analysis = analysis(item.getUserId(), item);
        return new WorkspaceMailItemResponse(
                item.getId(),
                mail.getId(),
                item.getFolder(),
                mail.getSenderEmail(),
                mail.getSubject(),
                summaryPreview(mail, analysis),
                analysis.category(),
                analysis.status(),
                item.getReadFlag(),
                item.getStarFlag(),
                item.getPriority(),
                mail.getHasAttachment(),
                item.getReceivedAt()
        );
    }

    private WorkspaceAnalysisResponse analysis(Long userId, MailboxItem item) {
        List<MailAiResult> results = aiResultMapper.listByMailAndUser(item.getMailId(), userId);
        String status = results.stream().findFirst().map(MailAiResult::getStatus).orElse(null);
        if (status == null) {
            status = analysisTaskService.latestStatus(userId, item.getId());
        }
        List<String> summaries = results.stream()
                .filter(result -> "SUMMARY".equalsIgnoreCase(result.getResultType()))
                .map(MailAiResult::getResultJson)
                .findFirst()
                .map(this::summaryLines)
                .orElse(List.of());
        boolean junk = "JUNK".equalsIgnoreCase(item.getFolder()) || results.stream()
                .anyMatch(result -> "JUNK".equalsIgnoreCase(result.getResultType()) && result.getResultJson().contains("true"));
        return new WorkspaceAnalysisResponse(status == null ? "PENDING" : status, summaries, null, junk, List.of());
    }

    private List<String> summaryLines(String resultJson) {
        String cleaned = resultJson == null ? "" : resultJson
                .replace('[', ' ')
                .replace(']', ' ')
                .replace('{', ' ')
                .replace('}', ' ')
                .replace('"', ' ')
                .trim();
        if (cleaned.isBlank()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String part : cleaned.split("\\\\n|;|,")) {
            String value = part.trim();
            if (!value.isBlank()) {
                lines.add(value.length() > 160 ? value.substring(0, 160) : value);
            }
            if (lines.size() == 3) {
                break;
            }
        }
        return lines;
    }

    private String summaryPreview(MailMessage mail, WorkspaceAnalysisResponse analysis) {
        String text = analysis.summary().isEmpty() ? mail.getContentText() : analysis.summary().get(0);
        if (text == null) {
            return "";
        }
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 120 ? oneLine.substring(0, 120) : oneLine;
    }

    private boolean matchesView(MailboxItem item, String view) {
        return switch (view) {
            case "sent" -> "SENT".equals(item.getFolder());
            case "trash" -> "TRASH".equals(item.getFolder());
            case "junk" -> "JUNK".equals(item.getFolder());
            case "today" -> item.getReceivedAt() != null && item.getReceivedAt().toLocalDate().equals(LocalDate.now());
            case "important" -> "HIGH".equalsIgnoreCase(item.getPriority()) || Boolean.TRUE.equals(item.getStarFlag());
            case "unread" -> !Boolean.TRUE.equals(item.getReadFlag());
            case "inbox" -> "INBOX".equals(item.getFolder());
            case "drafts" -> false;
            default -> "INBOX".equals(item.getFolder());
        };
    }

    private boolean matchesKeyword(MailboxItem item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        MailMessage mail = requireMail(item.getMailId());
        String needle = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(mail.getSenderEmail(), needle)
                || contains(mail.getSubject(), needle)
                || contains(mail.getContentText(), needle)
                || contains(mail.getContentHtml(), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private String normalizeView(String view) {
        return view == null || view.isBlank() ? "inbox" : view.trim().toLowerCase(Locale.ROOT);
    }

    private MailboxItem requireOwnedItem(Long userId, Long itemId) {
        MailboxItem item = mailboxMapper.selectById(itemId);
        if (item == null || !userId.equals(item.getUserId()) || Boolean.TRUE.equals(item.getDeletedFlag())) {
            throw new BusinessException(404, "Mailbox item not found");
        }
        return item;
    }

    private MailMessage requireMail(Long mailId) {
        MailMessage mail = mailMapper.selectById(mailId);
        if (mail == null) {
            throw new BusinessException(404, "Mail not found");
        }
        return mail;
    }
}
