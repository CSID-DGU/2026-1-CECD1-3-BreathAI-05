package com.breathAI.ttobagi_server.domain.faq.entity;

import com.breathAI.ttobagi_server.domain.auth.entity.User;
import com.breathAI.ttobagi_server.domain.dashboard.entity.AnalysisJob;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "gold_retrieve_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RetrieveLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "retrieve_id")
    private Long retrieveId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private AnalysisJob analysisJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @Column(name = "returned_faq_seq_num")
    private Integer returnedFaqSeqNum;

    @Column(name = "rerank_score", precision = 5, scale = 4)
    private BigDecimal rerankScore;

    @Column(name = "retrieved_at", nullable = false, updatable = false)
    private LocalDateTime retrievedAt;

    @PrePersist
    protected void onCreate() {
        retrievedAt = LocalDateTime.now();
    }

    @Builder
    public RetrieveLog(AnalysisJob analysisJob, User user, String queryText,
                       Integer returnedFaqSeqNum, BigDecimal rerankScore) {
        this.analysisJob = analysisJob;
        this.user = user;
        this.queryText = queryText;
        this.returnedFaqSeqNum = returnedFaqSeqNum;
        this.rerankScore = rerankScore;
    }
    
}
