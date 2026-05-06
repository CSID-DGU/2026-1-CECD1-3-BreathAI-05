package com.breathAI.ttobagi_server.domain.faq.entity;

import com.breathAI.ttobagi_server.domain.dashboard.entity.StatDate;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
    name = "gold_surge_stat",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_surge_date_period",
            columnNames = {"surge_id", "stat_date", "period_type"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurgeStat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "surge_stat_id")
    private Long surgeStatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "surge_id", nullable = false)
    private SurgeDetection surgeDetection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stat_date", referencedColumnName = "stat_date", nullable = false)
    private StatDate statDate;

    @Column(name = "period_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    @Column(name = "cnt", nullable = false)
    private Integer cnt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum PeriodType {
        BASE, PREV
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public SurgeStat(SurgeDetection surgeDetection, StatDate statDate,
                     PeriodType periodType, Integer cnt) {
        this.surgeDetection = surgeDetection;
        this.statDate = statDate;
        this.periodType = periodType;
        this.cnt = cnt;
    }

}
