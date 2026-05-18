package com.breathAI.ttobagi_server.domain.analysis.controller;

import com.breathAI.ttobagi_server.domain.analysis.client.AiPipelineClient.AiPipelineAcceptedResponse;
import com.breathAI.ttobagi_server.domain.analysis.service.AnalysisService;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/v1/dashboard/analyze")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * 분석 API 동작 확인용 endpoint.
     *
     * 예:
     * GET /api/v1/dashboard/analyze/health
     */
    @GetMapping("/health")
    public ResponseEntity<AnalysisHealthResponse> health() {
        return ResponseEntity.ok(
                new AnalysisHealthResponse(
                        "UP",
                        "analysis-controller"
                )
        );
    }

    /**
     * 새 분석 요청을 시작한다.
     *
     * 요청 형식:
     * multipart/form-data
     *
     * 필드:
     * - file
     * - isMaskingEnabled
     * - isTranslationEnabled
     * - periodStartDate
     * - periodEndDate
     *
     * 이 endpoint는 파일과 옵션을 받아 AI 서버의 /pipeline endpoint로 전달한다.
     */
    @PostMapping(
            value = "/new",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<AiPipelineAcceptedResponse> startAnalysis(
            @RequestPart("file") MultipartFile file,
            @ModelAttribute AnalysisStartRequest request
    ) {
        AiPipelineAcceptedResponse response = analysisService.startAnalysis(
                file,
                request.isMaskingEnabled(),
                request.isTranslationEnabled(),
                request.periodStartDate(),
                request.periodEndDate()
        );

        return ResponseEntity.accepted().body(response);
    }

    public record AnalysisStartRequest(
            @NotNull
            Boolean isMaskingEnabled,

            @NotNull
            Boolean isTranslationEnabled,

            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate periodStartDate,

            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate periodEndDate
    ) {
    }

    public record AnalysisHealthResponse(
            String status,
            String service
    ) {
    }
}