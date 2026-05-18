package com.breathAI.ttobagi_server.domain.analysis.client;

import com.breathAI.ttobagi_server.global.config.AiServerProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;

@Component
public class AiPipelineClient {

    private final RestClient restClient;

    public AiPipelineClient(
            RestClient.Builder restClientBuilder,
            AiServerProperties aiServerProperties
    ) {
        this.restClient = restClientBuilder
                .baseUrl(aiServerProperties.getBaseUrl())
                .build();
    }

    /**
     * Spring Boot 서버에서 FastAPI AI 서버의 /pipeline endpoint를 호출한다.
     *
     * AI 서버 요청 형식:
     * multipart/form-data
     *
     * 필드:
     * - file
     * - analysisId
     * - uploadId
     * - isMaskingEnabled
     * - isTranslationEnabled
     * - periodStartDate
     * - periodEndDate
     */
    public AiPipelineAcceptedResponse requestPipeline(
            MultipartFile file,
            Long analysisId,
            String uploadId,
            Boolean isMaskingEnabled,
            Boolean isTranslationEnabled,
            LocalDate periodStartDate,
            LocalDate periodEndDate
    ) {
        validateRequest(
                file,
                analysisId,
                uploadId,
                isMaskingEnabled,
                isTranslationEnabled,
                periodStartDate,
                periodEndDate
        );

        MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();

        multipartBody.add("file", buildFilePart(file));
        multipartBody.add("analysisId", analysisId.toString());
        multipartBody.add("uploadId", uploadId);
        multipartBody.add("isMaskingEnabled", isMaskingEnabled.toString());
        multipartBody.add("isTranslationEnabled", isTranslationEnabled.toString());
        multipartBody.add("periodStartDate", periodStartDate.toString());
        multipartBody.add("periodEndDate", periodEndDate.toString());

        return restClient.post()
                .uri("/pipeline")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipartBody)
                .retrieve()
                .body(AiPipelineAcceptedResponse.class);
    }

    private HttpEntity<ByteArrayResource> buildFilePart(MultipartFile file) {
        try {
            String originalFilename = Objects.requireNonNullElse(
                    file.getOriginalFilename(),
                    "uploaded-file"
            );

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("file", originalFilename);
            headers.setContentType(resolveContentType(file));

            return new HttpEntity<>(fileResource, headers);

        } catch (IOException e) {
            throw new IllegalStateException("AI 서버로 전달할 업로드 파일을 읽는 중 오류가 발생했습니다.", e);
        }
    }

    private MediaType resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private void validateRequest(
            MultipartFile file,
            Long analysisId,
            String uploadId,
            Boolean isMaskingEnabled,
            Boolean isTranslationEnabled,
            LocalDate periodStartDate,
            LocalDate periodEndDate
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("AI 서버로 전달할 파일이 비어 있습니다.");
        }

        if (analysisId == null) {
            throw new IllegalArgumentException("analysisId는 필수입니다.");
        }

        if (uploadId == null || uploadId.isBlank()) {
            throw new IllegalArgumentException("uploadId는 필수입니다.");
        }

        if (isMaskingEnabled == null) {
            throw new IllegalArgumentException("isMaskingEnabled는 필수입니다.");
        }

        if (isTranslationEnabled == null) {
            throw new IllegalArgumentException("isTranslationEnabled는 필수입니다.");
        }

        if (periodStartDate == null) {
            throw new IllegalArgumentException("periodStartDate는 필수입니다.");
        }

        if (periodEndDate == null) {
            throw new IllegalArgumentException("periodEndDate는 필수입니다.");
        }

        if (periodStartDate.isAfter(periodEndDate)) {
            throw new IllegalArgumentException("periodStartDate는 periodEndDate보다 늦을 수 없습니다.");
        }
    }

    public record AiPipelineAcceptedResponse(
            Long analysisId,
            String uploadId,
            String status,
            String message
    ) {
    }
}