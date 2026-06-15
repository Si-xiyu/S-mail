package com.smartmail.analysis.controller;

import com.smartmail.analysis.dto.AnalysisRetryResponse;
import com.smartmail.analysis.service.AnalysisService;
import com.smartmail.common.response.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/mail-items/{itemId}/retry")
    public ApiResponse<AnalysisRetryResponse> retry(@PathVariable Long itemId) {
        return ApiResponse.ok(analysisService.retryForMailItem(itemId));
    }
}
