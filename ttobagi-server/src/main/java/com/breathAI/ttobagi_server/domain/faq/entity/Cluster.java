package com.breathAI.ttobagi_server.domain.faq.entity;

import com.breathAI.ttobagi_server.domain.dashboard.entity.AnalysisJob;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

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
    },
    comment = "클러스터링 결과 (HDBSCAN)"
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cluster {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cluster_id")
    private Long clusterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cluster_analysis_id"))
    private AnalysisJob analysisJob;

    @Column(name = "cluster_label", nullable = false, columnDefinition = "INT COMMENT 'HDBSCAN 번호 (-1=노이즈)'")
    private Integer clusterLabel;

    @Column(name = "top_keywords", columnDefinition = "JSON COMMENT 'TF-IDF Top5 키워드 배열'")
    private String topKeywords;

    @Column(name = "cluster_name", length = 100, columnDefinition = "VARCHAR(100) COMMENT '클러스터 이름 (예: 냉난방 온도 조절)'")
    private String clusterName;

    @Column(name = "cluster_size", columnDefinition = "INT COMMENT '해당 클러스터에 포함된 로그 총 개수'")
    private Integer clusterSize;  

    @Column(name = "created_at", nullable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }  

    @Builder
    public Cluster(AnalysisJob analysisJob, Integer clusterLabel, String clusterName, String topKeywords, Integer clusterSize) {
        this.analysisJob = analysisJob;
        this.clusterLabel = clusterLabel;
        this.clusterName = clusterName;
        this.topKeywords = topKeywords;
        this.clusterSize = clusterSize;
    }
    
}
