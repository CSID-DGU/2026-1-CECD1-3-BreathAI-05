package com.breathAI.ttobagi_server.domain.faq.entity;

import com.breathAI.ttobagi_server.domain.dashboard.entity.AnalysisJob;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
    name = "gold_surge_detection",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_analysis_keyword",
            columnNames = {"analysis_id", "keyword"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurgeDetection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "surge_id")
    private Long surgeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisJob analysisJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prev_analysis_id")
    private AnalysisJob prevAnalysisJob;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;

    @Column(name = "is_new", nullable = false)
    private Boolean isNew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_cluster_id")
    private Cluster relatedCluster;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (this.isNew == null) {
            this.isNew = false;
        }
    }

    @Builder
    public SurgeDetection(AnalysisJob analysisJob, AnalysisJob prevAnalysisJob,
                          String keyword, boolean isNew, Cluster relatedCluster) {
        this.analysisJob = analysisJob;
        this.prevAnalysisJob = prevAnalysisJob;
        this.keyword = keyword;
        this.isNew = isNew;
        this.relatedCluster = relatedCluster;
    }

}
