package com.breathAI.ttobagi_server.domain.analysis.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard/analyze")
public class AiCallbackController {

    /**
     * AI 서버 분석 완료 callback 수신 endpoint.
     *
     * AI 서버는 분석이 끝난 뒤 아래 주소로 callback을 보낸다.
     *
     * POST /api/v1/dashboard/analyze/callback/{analysisId}
     *
     * 현재 단계에서는 Gold DB 저장 전,
     * callback payload 수신 여부를 확인하기 위해 Map 형태로 받는다.
     *
     * 추후 작업:
     * - AI_to_backend.md 기준 DTO 생성
     * - Gold DB Entity/Repository 연결
     * - 분석 상태 COMPLETED / FAILED 반영
     */
    @PostMapping("/callback/{analysisId}")
    public ResponseEntity<AiCallbackResponse> receiveCallback(
            @PathVariable Long analysisId,
            @RequestBody Map<String, Object> payload
    ) {
        System.out.println("[AI CALLBACK] callback 수신");
        System.out.println("[AI CALLBACK] path analysisId = " + analysisId);
        System.out.println("[AI CALLBACK] payload analysisId = " + payload.get("analysisId"));
        System.out.println("[AI CALLBACK] status = " + payload.get("status"));
        System.out.println("[AI CALLBACK] uploadId = " + payload.get("uploadId"));
        System.out.println("[AI CALLBACK] fileName = " + payload.get("fileName"));

        // TODO:
        // 1. payload를 AI_to_backend.md 기준 DTO로 변환
        // 2. Gold DB 저장
        // 3. analysis 상태 업데이트
        // 4. FAILED payload인 경우 에러 메시지 저장

        return ResponseEntity.ok(
                new AiCallbackResponse(
                        analysisId,
                        "RECEIVED",
                        "AI callback payload received successfully.",
                        LocalDateTime.now()
                )
        );
    }

    public record AiCallbackResponse(
            Long analysisId,
            String status,
            String message,
            LocalDateTime receivedAt
    ) {
    }
}