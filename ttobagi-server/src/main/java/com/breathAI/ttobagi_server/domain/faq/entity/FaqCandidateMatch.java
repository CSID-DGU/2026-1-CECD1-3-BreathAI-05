package com.breathAI.ttobagi_server.domain.faq.entity;

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
    name = "gold_faq_candidate_match",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_candidate_faq",
            columnNames = {"candidate_id", "matched_faq_seq_num"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqCandidateMatch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Long matchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private FaqCandidate candidate;

    @Column(name = "matched_faq_seq_num", nullable = false)
    private Integer matchedFaqSeqNum;

    @Column(name = "match_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal matchScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public FaqCandidateMatch(FaqCandidate candidate, Integer matchedFaqSeqNum, BigDecimal matchScore) {
        this.candidate = candidate;
        this.matchedFaqSeqNum = matchedFaqSeqNum;
        this.matchScore = matchScore;
    }

}
