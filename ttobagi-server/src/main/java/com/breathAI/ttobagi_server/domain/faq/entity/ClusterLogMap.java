package com.breathAI.ttobagi_server.domain.faq.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
    name = "gold_cluster_log_map",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_cluster_log",
            columnNames = {"cluster_id", "source_log_seq_num"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClusterLogMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "map_id")
    private Long mapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cluster_id", nullable = false)
    private Cluster cluster;

    @Column(name = "source_log_seq_num", nullable = false)
    private Integer sourceLogSeqNum;

    @Column(name = "log_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LogType logType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum LogType {
        CORRECT, LOW_QUALITY, UNANSWER
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public ClusterLogMap(Cluster cluster, Integer sourceLogSeqNum, LogType logType) {
        this.cluster = cluster;
        this.sourceLogSeqNum = sourceLogSeqNum;
        this.logType = logType;
    }
}