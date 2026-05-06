package com.breathAI.ttobagi_server.domain.faq.entity;

import com.breathAI.ttobagi_server.domain.dashboard.entity.AnalysisJob;
import com.breathAI.ttobagi_server.domain.dashboard.entity.UsageStat;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(
    name = "gold_performance_comparison",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_analysis", 
            columnNames = {"analysis_id"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerformanceComparison {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comparison_id")
    private Long comparisonId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisJob analysisJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "before_stat_date", referencedColumnName = "stat_date", nullable = false)    private UsageStat usageStat;

    @Column(name = "after_unanswer_cnt")
    private Integer afterUnanswerCnt;

    @Column(name = "after_unanswer_rate", precision = 5, scale = 2)
    private BigDecimal afterUnanswerRate;

    @Column(name = "accuracy_gain", precision = 5, scale = 2)
    private BigDecimal accuracyGain;

    @Column(name = "false_positive_rate", precision = 5, scale = 2)
    private BigDecimal falsePositiveRate;

    @Column(name = "eval_threshold", precision = 4, scale = 3)
    private BigDecimal evalThreshold;

    @Column(name = "actual_unanswer_cnt")
    private Integer actualUnanswerCnt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public PerformanceComparison(AnalysisJob analysisJob, UsageStat usageStat,
                                 Integer afterUnanswerCnt, BigDecimal afterUnanswerRate,
                                 BigDecimal accuracyGain, BigDecimal falsePositiveRate,
                                 BigDecimal evalThreshold) {
        this.analysisJob = analysisJob;
        this.usageStat = usageStat;
        this.afterUnanswerCnt = afterUnanswerCnt;
        this.afterUnanswerRate = afterUnanswerRate;
        this.accuracyGain = accuracyGain;
        this.falsePositiveRate = falsePositiveRate;
        this.evalThreshold = evalThreshold;
    }

    public void updateActualUnanswerCnt(Integer actualUnanswerCnt) {
        this.actualUnanswerCnt = actualUnanswerCnt;
    }

}
