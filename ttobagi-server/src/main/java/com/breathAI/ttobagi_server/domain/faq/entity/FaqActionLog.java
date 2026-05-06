package com.breathAI.ttobagi_server.domain.faq.entity;

import com.breathAI.ttobagi_server.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "gold_faq_action_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqActionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private FaqCandidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acted_by", nullable = false)
    private User actedBy;

    @Column(name = "action", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_status", length = 20)
    private FaqCandidate.ReviewStatus beforeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "after_status", length = 20)
    private FaqCandidate.ReviewStatus afterStatus;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Action {
        ACCEPT, REJECT, APPLY, REVERT
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }


    @Builder
    public FaqActionLog(FaqCandidate candidate, User actedBy, Action action,
                        FaqCandidate.ReviewStatus beforeStatus, FaqCandidate.ReviewStatus afterStatus, String note) {
        this.candidate = candidate;
        this.actedBy = actedBy;
        this.action = action;
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.note = note;
    }

}
