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
@Table(name = "gold_faq_edit_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqEditHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "source_seq_num", nullable = false)
    private Integer sourceSeqNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private AnalysisJob analysisJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by", nullable = false)
    private User editedBy;

    @Column(name = "before_question", columnDefinition = "TEXT")
    private String beforeQuestion;

    @Column(name = "before_answer", columnDefinition = "LONGTEXT")
    private String beforeAnswer;

    @Column(name = "after_question", columnDefinition = "TEXT")
    private String afterQuestion;

    @Column(name = "after_answer", columnDefinition = "LONGTEXT")
    private String afterAnswer;

    @Column(name = "edit_reason", columnDefinition = "TEXT")
    private String editReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public FaqEditHistory(Integer sourceSeqNum, AnalysisJob analysisJob,
                          User editedBy, String beforeQuestion, String beforeAnswer,
                          String afterQuestion, String afterAnswer, String editReason) {
        this.sourceSeqNum = sourceSeqNum;
        this.analysisJob = analysisJob;
        this.editedBy = editedBy;
        this.beforeQuestion = beforeQuestion;
        this.beforeAnswer = beforeAnswer;
        this.afterQuestion = afterQuestion;
        this.afterAnswer = afterAnswer;
        this.editReason = editReason;
    }

}
