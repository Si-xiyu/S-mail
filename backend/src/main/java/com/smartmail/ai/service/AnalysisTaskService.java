package com.smartmail.ai.service;

import com.smartmail.ai.entity.AiAnalysisTask;
import com.smartmail.ai.mapper.AiAnalysisTaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AnalysisTaskService {
    private final AiAnalysisTaskMapper taskMapper;

    public AnalysisTaskService(AiAnalysisTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public void enqueuePending(Long userId, Long itemId, Long mailId) {
        LocalDateTime now = LocalDateTime.now();
        AiAnalysisTask task = new AiAnalysisTask();
        task.setUserId(userId);
        task.setItemId(itemId);
        task.setMailId(mailId);
        task.setStatus("PENDING");
        task.setAttemptCount(0);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        taskMapper.insert(task);
    }

    public String latestStatus(Long userId, Long itemId) {
        AiAnalysisTask task = taskMapper.findLatestByItemAndUser(itemId, userId);
        return task == null ? null : task.getStatus();
    }
}
