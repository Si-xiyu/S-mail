package com.smartmail.analysis.service;

import com.smartmail.ai.service.AiService;
import com.smartmail.analysis.dto.AnalysisRetryResponse;
import com.smartmail.analysis.entity.AiAnalysisTask;
import com.smartmail.analysis.mapper.AiAnalysisTaskMapper;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.UserContext;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private static final int MAX_BATCH_SIZE = 5;
    private static final int MAX_RETRY = 3;

    private final AiAnalysisTaskMapper taskMapper;
    private final AiService aiService;
    private final MailboxItemMapper mailboxItemMapper;

    public AnalysisService(
            AiAnalysisTaskMapper taskMapper,
            AiService aiService,
            MailboxItemMapper mailboxItemMapper
    ) {
        this.taskMapper = taskMapper;
        this.aiService = aiService;
        this.mailboxItemMapper = mailboxItemMapper;
    }

    public void createTask(Long mailId, Long userId) {
        AiAnalysisTask task = new AiAnalysisTask();
        task.setMailId(mailId);
        task.setUserId(userId);
        task.setTaskType("FULL_ANALYSIS");
        task.setStatus("PENDING");
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

    private void processOne(AiAnalysisTask task) {
        try {
            aiService.runMailTask(task.getMailId(), task.getUserId(), "analyze");
            task.setStatus("SUCCEEDED");
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
        AiAnalysisTask existing = taskMapper.findLatestByMailAndUser(item.getMailId(), userId);
        if (existing != null && "PENDING".equals(existing.getStatus())) {
            return new AnalysisRetryResponse("PENDING");
        }
        createTask(item.getMailId(), userId);
        return new AnalysisRetryResponse("PENDING");
    }
}
