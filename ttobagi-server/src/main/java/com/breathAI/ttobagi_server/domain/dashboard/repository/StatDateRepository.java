package com.breathAI.ttobagi_server.domain.dashboard.repository;

import com.breathAI.ttobagi_server.domain.dashboard.entity.StatDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StatDateRepository extends JpaRepository<StatDate, LocalDate> {
    List<StatDate> findByStatYearOrderByStatDateAsc(int statYear);
    List<StatDate> findByStatYearAndStatMonthOrderByStatDateAsc(int statYear, int statMonth);
    List<StatDate> findByStatDateBetweenOrderByStatDateAsc(LocalDate startDate, LocalDate endDate);
    boolean existsByStatDate(LocalDate statDate);
}