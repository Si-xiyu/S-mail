package com.smartmail.analysis.task;

import com.smartmail.analysis.service.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnalysisTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTaskScheduler.class);

    private final AnalysisService analysisService;

    public AnalysisTaskScheduler(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @Scheduled(fixedDelay = 10000)
    public void poll() {
        log.debug("Polling pending analysis tasks...");
        analysisService.processNextBatch();
    }
}
