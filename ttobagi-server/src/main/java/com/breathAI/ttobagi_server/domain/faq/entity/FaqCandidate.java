package com.breathAI.ttobagi_server.domain.faq.entity;

import com.breathAI.ttobagi_server.domain.auth.entity.User;
import com.breathAI.ttobagi_server.domain.dashboard.entity.AnalysisJob;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "gold_faq_candidate")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqCandidate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "candidate_id")
    private Long candidateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisJob analysisJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cluster_id")
    private Cluster cluster;

    @Column(name = "candidate_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CandidateType candidateType;

    @Column(name = "standard_question", nullable = false, columnDefinition = "TEXT")
    private String standardQuestion;

    @Column(name = "answer_draft", columnDefinition = "TEXT")
    private String answerDraft;

    @Column(name = "review_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReviewStatus reviewStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount;

    @Column(name = "representative_keywords", columnDefinition = "JSON")
    private String representativeKeywords;

    public enum CandidateType {
        NEW, EXPAND
    }

    public enum ReviewStatus {
        PENDING, ACCEPTED, REJECTED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (reviewStatus == null) reviewStatus = ReviewStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public FaqCandidate(AnalysisJob analysisJob, Cluster cluster, 
                        CandidateType candidateType, String standardQuestion,
                        String answerDraft, Integer occurrenceCount, String representativeKeywords) {
        this.analysisJob = analysisJob;
        this.cluster = cluster;
        this.candidateType = candidateType;
        this.standardQuestion = standardQuestion;
        this.answerDraft = answerDraft;
        this.occurrenceCount = occurrenceCount;
        this.representativeKeywords = representativeKeywords;
        this.reviewStatus = ReviewStatus.PENDING;
    }

    public void accept(User reviewer) {
        validatePendingStatus();
        this.reviewStatus = ReviewStatus.ACCEPTED;
        this.reviewedBy = reviewer;
    }

    public void reject(User reviewer) {
        validatePendingStatus();
        this.reviewStatus = ReviewStatus.REJECTED;
        this.reviewedBy = reviewer;
    }

    private void validatePendingStatus() {
        if (this.reviewStatus != ReviewStatus.PENDING) {
            throw new IllegalStateException("이미 검토 완료된 항목입니다.");
        }
    }

}
