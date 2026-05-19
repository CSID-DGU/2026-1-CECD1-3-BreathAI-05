package com.breathAI.ttobagi_server.domain.analysis.service;

import com.breathAI.ttobagi_server.domain.analysis.client.AiPipelineClient;
import com.breathAI.ttobagi_server.domain.analysis.client.AiPipelineClient.AiPipelineAcceptedResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class AnalysisService {

    private final AiPipelineClient aiPipelineClient;

    public AnalysisService(AiPipelineClient aiPipelineClient) {
        this.aiPipelineClient = aiPipelineClient;
    }

    /**
     * 대시보드에서 새 분석 요청을 시작한다.
     *
     * 현재 단계에서는 분석 요청 정보를 DB에 저장하지 않고,
     * 즉시 AI 서버 /pipeline으로 전달한다.
     *
     * 추후 추가 예정:
     * - analysis 요청 이력 DB 저장
     * - 상태값 PROCESSING / COMPLETED / FAILED 관리
     * - uploadId와 실제 업로드 파일 메타데이터 연결
     */
    public AiPipelineAcceptedResponse startAnalysis(
            MultipartFile file,
            Boolean isMaskingEnabled,
            Boolean isTranslationEnabled,
            LocalDate periodStartDate,
            LocalDate periodEndDate
    ) {
        Long analysisId = generateAnalysisId();
        String uploadId = UUID.randomUUID().toString();

        return aiPipelineClient.requestPipeline(
                file,
                analysisId,
                uploadId,
                isMaskingEnabled,
                isTranslationEnabled,
                periodStartDate,
                periodEndDate
        );
    }

    /**
     * 임시 analysisId 생성 로직.
     *
     * 형식:
     * yyyyMMddHHmmss 형태의 Long 값
     *
     * 예:
     * 20260419093015
     *
     * 추후 DB에서 auto increment 또는 별도 분석 테이블 ID를 사용하면
     * 이 메서드는 제거하거나 대체하면 된다.
     */
    private Long generateAnalysisId() {
        return Long.parseLong(
                java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        );
    }
}