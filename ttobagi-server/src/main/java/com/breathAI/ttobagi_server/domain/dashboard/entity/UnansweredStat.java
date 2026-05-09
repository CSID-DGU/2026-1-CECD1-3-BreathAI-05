package com.breathAI.ttobagi_server.domain.dashboard.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "gold_unanswer_stat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UnansweredStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stat_id")
    private Long statId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "analysis_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_unanswer_stat_analysis_id")
    )
    private AnalysisJob analysisJob;

    @Column(name = "reason", nullable = false, length = 50,
            columnDefinition = "VARCHAR(50) COMMENT '미답변 사유 (키워드 미등록 / 외국어 질의 / 보안/정책상 답변 불가 / 기타)'")
    private String reason;

    @Column(name = "current_unanswer_rate", precision = 5, scale = 2,
            columnDefinition = "DECIMAL(5,2) COMMENT '분석 결과 적용 후 예상 미답변율 (%)'")
    private BigDecimal currentUnanswerRate;

    @Column(name = "unanswer_count", nullable = false,
            columnDefinition = "INT NOT NULL DEFAULT 0 COMMENT '해당 사유별 집계 건수'")
    @ColumnDefault("0")
    private Integer unanswerCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.unanswerCount == null) this.unanswerCount = 0;
    }

    @Builder
    public UnansweredStat(AnalysisJob analysisJob, String reason,
                          BigDecimal currentUnanswerRate, Integer unanswerCount) {
        this.analysisJob = analysisJob;
        this.reason = reason;
        this.currentUnanswerRate = currentUnanswerRate;
        this.unanswerCount = unanswerCount != null ? unanswerCount : 0;
    }
}