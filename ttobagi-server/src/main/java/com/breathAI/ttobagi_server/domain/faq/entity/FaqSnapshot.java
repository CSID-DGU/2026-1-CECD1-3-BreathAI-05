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
    name = "gold_faq_snapshot",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_analysis_faq",
            columnNames = {"analysis_id", "source_seq_num"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisJob analysisJob;

    @Column(name = "source_seq_num", nullable = false)
    private Integer sourceSeqNum;

    @Column(name = "q_type")
    private Integer qType;

    @Column(name = "ci_question", columnDefinition = "TEXT")
    private String ciQuestion;

    @Column(name = "ci_answer0", columnDefinition = "LONGTEXT")
    private String ciAnswer0;

    @Column(name = "qa_cnt")
    private Integer qaCnt;

    @Column(name = "snapshotted_at", nullable = false, updatable = false)
    private LocalDateTime snapshottedAt;

    @PrePersist
    protected void onCreate() {
        snapshottedAt = LocalDateTime.now();
    }

    @Builder
    public FaqSnapshot(AnalysisJob analysisJob, Integer sourceSeqNum,
                        Integer qType, String ciQuestion, String ciAnswer0, Integer qaCnt) {
        this.analysisJob = analysisJob;
        this.sourceSeqNum = sourceSeqNum;
        this.qType = qType;
        this.ciQuestion = ciQuestion;
        this.ciAnswer0 = ciAnswer0;
        this.qaCnt = qaCnt;
    }

}
