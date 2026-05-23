package com.breathAI.ttobagi_server.domain.faq.dto;

import com.breathAI.ttobagi_server.domain.faq.entity.FaqCandidate.CandidateType;
import com.breathAI.ttobagi_server.domain.faq.entity.FaqCandidate.ReviewStatus;
import com.breathAI.ttobagi_server.domain.faq.entity.SynonymCandidate.SynonymType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqCandidateListResponse {
    private Long analysisId;
    private List<FaqCandidateItem> recommendations;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaqCandidateItem {
        private Long clusterId;
        private Integer clusterLabel;
        private Long candidateId;
        private CandidateType candidateType;
        private String standardQuestion;
        private String answerDraft;
        private ReviewStatus reviewStatus;
        private List<String> representativeKeywords;
        private Integer occurrenceCount;
        private List<SynonymItem> synonyms;

        @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'",
            timezone = "UTC"
        )
        private LocalDateTime createdAt;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SynonymItem {
            private String text;
            private SynonymType type;
        }
    }
}