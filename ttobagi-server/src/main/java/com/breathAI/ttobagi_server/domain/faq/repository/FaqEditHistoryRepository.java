package com.breathAI.ttobagi_server.domain.faq.repository;

import com.breathAI.ttobagi_server.domain.faq.entity.FaqEditHistory;
import com.breathAI.ttobagi_server.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query; 
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FaqEditHistoryRepository extends JpaRepository<FaqEditHistory, Long> {

    List<FaqEditHistory> findByFaq_FaqId(Long faqId);
    List<FaqEditHistory> findByFaq_FaqIdOrderByCreatedAtDesc(Long faqId);

    List<FaqEditHistory> findByAnalysisJobAnalysisId(Long analysisId);
    List<FaqEditHistory> findByEditedBy_UserId(Long userId);
    List<FaqEditHistory> findByAnalysisJob_AnalysisIdAndEditedBy_UserId(Long analysisId, Long userId);

    @Modifying
    @Query("UPDATE FaqEditHistory f SET f.editedBy = null WHERE f.editedBy = :user")
    void clearEditedBy(@Param("user") User user);
}