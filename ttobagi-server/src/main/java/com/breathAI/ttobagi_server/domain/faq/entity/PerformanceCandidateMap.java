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
    name = "gold_performance_candidate_map",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_comparison_candidate",
            columnNames = {"comparison_id", "candidate_id"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerformanceCandidateMap {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "map_id")
    private Long mapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comparison_id", nullable = false)
    private PerformanceComparison comparison;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private FaqCandidate candidate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public PerformanceCandidateMap(PerformanceComparison comparison, FaqCandidate candidate) {
        this.comparison = comparison;
        this.candidate = candidate;
    }

}
