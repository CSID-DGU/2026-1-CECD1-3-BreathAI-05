package com.breathAI.ttobagi_server.domain.dashboard.controller;

import com.breathAI.ttobagi_server.domain.dashboard.dto.*;
import com.breathAI.ttobagi_server.domain.dashboard.service.DashboardService;
import com.breathAI.ttobagi_server.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.breathAI.ttobagi_server.global.util.SseEmitterManager;


import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final SseEmitterManager sseEmitterManager;

    // 챗봇 사용량 조회
    // GET /api/v1/dashboard/usage?year=2026&month=4
    @GetMapping("/usage")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UsageResponse>> getUsage(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getUsage(year, month),
                "챗봇 사용량 조회가 완료되었습니다."));
    }

    // 데이터 분석 결과 조회
    // GET /api/v1/dashboard/analyze/results?startDate=...&endDate=...
    @GetMapping("/analyze/results")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AnalyzeResultResponse>> getAnalyzeResult(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getAnalyzeResult(startDate, endDate),
                "대시보드 분석 데이터 조회가 완료되었습니다."));
    }

    // 파일별 분석 결과 조회
    // GET /api/v1/dashboard/analyze/{analysisId}/result
    @GetMapping("/analyze/{analysisId}/result")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<FileAnalyzeResultResponse>> getFileAnalyzeResult(
            @PathVariable Long analysisId) {
        
        return ResponseEntity.ok(ApiResponse.success(
            dashboardService.getFileAnalyzeResult(analysisId),
            "업로드 파일별 분석 결과 조회가 완료되었습니다."));
    }

    // 파일 업로드 + 분석 시작 (통합)
    // POST /api/v1/dashboard/analyze/upload
    @PostMapping("/analyze/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NewAnalyzeResponse>> uploadAndStartAnalysis(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "periodStartDate", required = false) String periodStartDate,
            @RequestParam(value = "periodEndDate", required = false) String periodEndDate,
            @RequestParam(value = "isMaskingEnabled", defaultValue = "true") boolean isMaskingEnabled,
            @RequestParam(value = "isTranslationEnabled", defaultValue = "false") boolean isTranslationEnabled,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.uploadAndStartAnalysis(file, email, periodStartDate, periodEndDate, isMaskingEnabled, isTranslationEnabled),
                "파일 업로드 및 AI 분석 파이프라인을 시작합니다."));
    }

    // 분석 진행 상태 조회
    // GET /api/v1/dashboard/analyze/{analysisId}
    @GetMapping("/analyze/{analysisId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AnalyzeStatusResponse>> getAnalysisStatus(
            @PathVariable Long analysisId) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getAnalysisStatus(analysisId),
                "현재 분석 상태 진행을 조회합니다."));
    }

    // 분석 이력 목록 조회
    // GET /api/v1/dashboard/analyze/history
    @GetMapping("/analyze/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AnalyzeHistoryResponse>> getAnalysisHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getAnalysisHistory(page, size),
                "분석 이력 목록 조회가 완료되었습니다."));
    }

    // FastAPI 분석 완료 콜백
    // POST /api/v1/dashboard/analyze/callback/{analysisId}
    @PostMapping("/analyze/callback/{analysisId}")
    public ResponseEntity<ApiResponse<Void>> receiveAnalysisCallback(
            @PathVariable Long analysisId,
            @RequestBody AnalysisCallbackRequest request) {
        dashboardService.handleAnalysisCallback(analysisId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // Spring -> React 콜백 전달
    @GetMapping(value = "/analyze/stream/{analysisId}", produces = "text/event-stream")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public SseEmitter streamAnalysisStatus(@PathVariable Long analysisId) {
        return sseEmitterManager.create(analysisId);
    }
}