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
    name = "gold_cluster",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_analysis_label",
            columnNames = {"analysis_id", "cluster_label"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cluster {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cluster_id")
    private Long clusterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisJob analysisJob;

    @Column(name = "cluster_label", nullable = false)
    private Integer clusterLabel;

    @Column(name = "top_keywords", columnDefinition = "JSON")
    private String topKeywords;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public Cluster(AnalysisJob analysisJob, Integer clusterLabel, String topKeywords) {
        this.analysisJob = analysisJob;
        this.clusterLabel = clusterLabel;
        this.topKeywords = topKeywords;
    }
    
}
