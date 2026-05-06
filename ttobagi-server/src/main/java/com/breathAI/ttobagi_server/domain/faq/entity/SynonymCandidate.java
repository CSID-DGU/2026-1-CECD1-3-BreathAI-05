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
    name = "gold_synonym_candidate",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_candidate_synonym",
            columnNames = {"candidate_id", "synonym_text"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SynonymCandidate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "synonym_id")
    private Long synonymId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private FaqCandidate candidate;

    @Column(name = "synonym_text", nullable = false, length = 100)
    private String synonymText;

    @Column(name = "synonym_type", length = 20)
    @Enumerated(EnumType.STRING)
    private SynonymType synonymType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum SynonymType {
        TYPO, ABBR, SYNONYM
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public SynonymCandidate(FaqCandidate candidate, String synonymText, SynonymType synonymType) {
        this.candidate = candidate;
        this.synonymText = synonymText;
        this.synonymType = synonymType;
    }
    
} 
