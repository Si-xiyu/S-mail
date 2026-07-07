package com.smartmail.analysis.service;

import com.smartmail.ai.service.AiService;
import com.smartmail.ai.entity.MailAiResult;
import com.smartmail.ai.mapper.MailAiResultMapper;
import com.smartmail.analysis.dto.AnalysisRetryResponse;
import com.smartmail.analysis.entity.AiAnalysisTask;
import com.smartmail.analysis.mapper.AiAnalysisTaskMapper;
import com.smartmail.attachment.entity.MailAttachment;
import com.smartmail.attachment.mapper.MailAttachmentMapper;
import com.smartmail.category.entity.MailCategory;
import com.smartmail.category.mapper.MailCategoryMapper;
import com.smartmail.category.service.CategoryService;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.UserContext;
import com.smartmail.common.util.JsonUtil;
import com.smartmail.mail.entity.MailMessage;
import com.smartmail.mail.entity.MailRecipient;
import com.smartmail.mail.mapper.MailMessageMapper;
import com.smartmail.mail.mapper.MailRecipientMapper;
import com.smartmail.mailbox.entity.MailboxItem;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import com.smartmail.user.service.UserSettingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private static final int MAX_BATCH_SIZE = 5;
    private static final int MAX_RETRY = 3;

    private final AiAnalysisTaskMapper taskMapper;
    private final AiService aiService;
    private final MailboxItemMapper mailboxItemMapper;
    private final MailMessageMapper mailMessageMapper;
    private final MailRecipientMapper recipientMapper;
    private final MailAttachmentMapper attachmentMapper;
    private final MailCategoryMapper categoryMapper;
    private final CategoryService categoryService;
    private final MailAiResultMapper aiResultMapper;

    private final UserSettingService userSettingService;
    private final boolean agentEnabled;

    public AnalysisService(
            AiAnalysisTaskMapper taskMapper,
            AiService aiService,
            MailboxItemMapper mailboxItemMapper,
            MailMessageMapper mailMessageMapper,
            MailRecipientMapper recipientMapper,
            MailAttachmentMapper attachmentMapper,
            MailCategoryMapper categoryMapper,
            CategoryService categoryService,
            MailAiResultMapper aiResultMapper,
            UserSettingService userSettingService,
            @Value("${smartmail.ai.enabled}") boolean agentEnabled
    ) {
        this.taskMapper = taskMapper;
        this.aiService = aiService;
        this.mailboxItemMapper = mailboxItemMapper;
        this.mailMessageMapper = mailMessageMapper;
        this.recipientMapper = recipientMapper;
        this.attachmentMapper = attachmentMapper;
        this.categoryMapper = categoryMapper;
        this.categoryService = categoryService;
        this.aiResultMapper = aiResultMapper;
        this.userSettingService = userSettingService;
        this.agentEnabled = agentEnabled;
    }

    public void createTask(Long itemId, Long mailId, Long userId) {
        boolean aiEnabled = isAiEnabledFor(userId);
        AiAnalysisTask task = new AiAnalysisTask();
        task.setItemId(itemId);
        task.setMailId(mailId);
        task.setUserId(userId);
        task.setTaskType("FULL_ANALYSIS");
        task.setStatus(aiEnabled ? "PENDING" : "DISABLED");
        task.setRetryCount(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
    }

    public void processNextBatch() {
        try {
            var tasks = taskMapper.listPending(MAX_BATCH_SIZE);
            for (AiAnalysisTask task : tasks) {
                processOne(task);
            }
        } catch (Exception e) {
            log.error("Analysis batch processing error", e);
        }
    }

    @Transactional
    private void processOne(AiAnalysisTask task) {
        try {
            Map<String, Object> response = aiService.analyzeMail(buildPluginRequest(task));
            String status = stringValue(response.get("status"), "FAILED");
            if ("SUCCEEDED".equals(status) || "PARTIAL".equals(status)) {
                saveAnalysisResult(task, response, status);
                saveSummaryResult(task, response);
                applyAnalysisOutcome(task, response);
                task.setStatus("SUCCEEDED");
            } else if ("DISABLED".equals(status)) {
                task.setStatus("DISABLED");
            } else {
                throw new BusinessException(502, stringValue(response.get("message"), "Agent Plugin analysis failed"));
            }
        } catch (Exception e) {
            log.warn("AI analysis failed for mailId={} userId={}: {}", task.getMailId(), task.getUserId(), e.getMessage());
            int retries = task.getRetryCount() + 1;
            if (retries >= MAX_RETRY) {
                task.setStatus("FAILED");
                task.setErrorMessage(e.getMessage());
            } else {
                task.setStatus("PENDING");
            }
            task.setRetryCount(retries);
        }
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    public AnalysisRetryResponse retryForMailItem(Long itemId) {
        Long userId = UserContext.requireUserId();
        var item = mailboxItemMapper.selectById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(404, "邮件条目不存在");
        }
        AiAnalysisTask existing = taskMapper.findLatestByItemAndUser(itemId, userId);
        if (existing != null && "PENDING".equals(existing.getStatus())) {
            return new AnalysisRetryResponse("PENDING");
        }
        if (!isAiEnabledFor(userId)) {
            createTask(item.getId(), item.getMailId(), userId);
            return new AnalysisRetryResponse("DISABLED");
        }
        createTask(item.getId(), item.getMailId(), userId);
        return new AnalysisRetryResponse("PENDING");
    }

    private Map<String, Object> buildPluginRequest(AiAnalysisTask task) {
        MailboxItem item = mailboxItemMapper.selectById(task.getItemId());
        MailMessage mail = mailMessageMapper.selectById(task.getMailId());
        if (item == null || mail == null) {
            throw new BusinessException(404, "分析任务对应邮件不存在");
        }
        List<String> recipients = recipientMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MailRecipient>()
                                .eq("mail_id", mail.getId())
                ).stream()
                .map(MailRecipient::getRecipientEmail)
                .toList();
        List<Map<String, Object>> attachments = attachmentMapper.listByMailId(mail.getId()).stream()
                .map(this::attachmentPayload)
                .toList();
        List<Map<String, Object>> categories = categoryMapper.listByUser(task.getUserId()).stream()
                .map(category -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", category.getId());
                    row.put("name", category.getName());
                    return row;
                })
                .toList();

        Map<String, Object> mailPayload = new LinkedHashMap<>();
        mailPayload.put("mailId", mail.getId());
        mailPayload.put("senderEmail", mail.getSenderEmail());
        mailPayload.put("senderDisplayName", mail.getSenderEmail());
        mailPayload.put("recipients", recipients);
        mailPayload.put("subject", mail.getSubject());
        mailPayload.put("contentText", mail.getContentText());
        mailPayload.put("contentHtml", mail.getContentHtml());
        mailPayload.put("attachments", attachments);
        mailPayload.put("sentAt", mail.getSentAt());

        var setting = userSettingService.ensure(task.getUserId());
        boolean aiEnabled = agentEnabled && Boolean.TRUE.equals(setting.getAiEnabled());
        String provider = "RULES";

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("taskId", task.getId());
        request.put("userId", task.getUserId());
        request.put("mailItemId", task.getItemId());
        request.put("mail", mailPayload);
        request.put("userCategories", categories);
        request.put("behaviorSignals", Map.of(
                "frequentSenders", List.of(),
                "recentRepliedSenders", List.of(),
                "recentMarkedJunkSenders", List.of()
        ));
        request.put("pluginConfig", Map.of(
                "aiPluginEnabled", aiEnabled,
                "provider", provider,
                "llmEnabled", false,
                "ragEnabled", false
        ));
        return request;
    }

    private Map<String, Object> attachmentPayload(MailAttachment attachment) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fileName", attachment.getOriginalName());
        row.put("mimeType", attachment.getMimeType());
        row.put("fileSize", attachment.getFileSize());
        return row;
    }

    private void saveAnalysisResult(AiAnalysisTask task, Map<String, Object> response, String status) {
        MailAiResult result = new MailAiResult();
        result.setMailId(task.getMailId());
        result.setUserId(task.getUserId());
        result.setResultType("ANALYSIS");
        result.setResultJson(JsonUtil.toJson(response));
        result.setStatus(status);
        result.setCreatedAt(LocalDateTime.now());
        result.setUpdatedAt(LocalDateTime.now());
        aiResultMapper.insert(result);
    }

    @SuppressWarnings("unchecked")
    private void saveSummaryResult(AiAnalysisTask task, Map<String, Object> response) {
        Object summaryObj = response.get("summary");
        if (!(summaryObj instanceof List<?> summaryList) || summaryList.isEmpty()) {
            return;
        }
        List<String> summary = summaryList.stream()
                .map(String::valueOf)
                .limit(3)
                .toList();
        MailAiResult result = new MailAiResult();
        result.setMailId(task.getMailId());
        result.setUserId(task.getUserId());
        result.setResultType("SUMMARY");
        result.setResultJson(JsonUtil.toJson(Map.of("summary", summary)));
        result.setStatus("SUCCEEDED");
        result.setCreatedAt(LocalDateTime.now());
        result.setUpdatedAt(LocalDateTime.now());
        aiResultMapper.insert(result);
    }

    @SuppressWarnings("unchecked")
    private void applyAnalysisOutcome(AiAnalysisTask task, Map<String, Object> response) {
        MailboxItem item = mailboxItemMapper.selectById(task.getItemId());
        if (item == null) {
            return;
        }
        String priority = stringValue(response.get("priority"), null);
        if (priority != null && Set.of("LOW", "NORMAL", "HIGH", "URGENT").contains(priority)) {
            item.setPriority(priority);
        }
        if (Boolean.TRUE.equals(response.get("junk"))) {
            item.setFolder("JUNK");
        }
        item.setUpdatedAt(LocalDateTime.now());
        mailboxItemMapper.updateById(item);

        Object categoryObj = response.get("category");
        if (categoryObj instanceof Map<?, ?> category) {
            Long categoryId = longValue(category.get("id"));
            if (categoryId != null) {
                categoryService.assignCategoryForUser(task.getMailId(), task.getUserId(), categoryId, "AI");
            } else {
                String name = stringValue(category.get("name"), null);
                MailCategory fallback = name == null ? null : categoryService.findDefaultCategory(task.getUserId(), name);
                if (fallback != null) {
                    categoryService.assignCategoryForUser(task.getMailId(), task.getUserId(), fallback.getId(), "AI");
                }
            }
        }
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private boolean isAiEnabledFor(Long userId) {
        return agentEnabled && Boolean.TRUE.equals(userSettingService.ensure(userId).getAiEnabled());
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
