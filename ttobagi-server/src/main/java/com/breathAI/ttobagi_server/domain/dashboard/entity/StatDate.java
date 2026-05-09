package com.breathAI.ttobagi_server.domain.dashboard.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "gold_stat_date")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatDate {
    
    @Id
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "stat_year", nullable = false)
    private Integer statYear;

    @Column(name = "stat_month", nullable = false)
    private Integer statMonth;

    @Builder
    public StatDate(LocalDate statDate) {
        this.statDate = statDate;
        this.statYear = statDate.getYear();
        this.statMonth = statDate.getMonthValue();
    }
}