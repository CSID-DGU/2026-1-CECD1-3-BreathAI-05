package com.breathAI.ttobagi_server.domain.faq.service;

import com.breathAI.ttobagi_server.domain.auth.entity.User;
import com.breathAI.ttobagi_server.domain.auth.repository.UserRepository;
import com.breathAI.ttobagi_server.domain.faq.dto.*;
import com.breathAI.ttobagi_server.domain.faq.entity.*;
import com.breathAI.ttobagi_server.domain.faq.entity.FaqCandidate.ReviewStatus;
import com.breathAI.ttobagi_server.domain.faq.repository.*;
import com.breathAI.ttobagi_server.global.exception.CustomException;
import com.breathAI.ttobagi_server.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;
    private final FaqCandidateRepository faqCandidateRepository;
    private final FaqCandidateMatchRepository faqCandidateMatchRepository;
    private final SynonymCandidateRepository synonymCandidateRepository;
    private final FaqActionLogRepository faqActionLogRepository;
    private final FaqEditHistoryRepository faqEditHistoryRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // FAQ 리스트 조회
    @Transactional(readOnly = true)
    public FaqListResponse getFaqList(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Faq> faqPage = faqRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageRequest);

        List<FaqListResponse.FaqItem> items = faqPage.getContent().stream()
                .map(f -> FaqListResponse.FaqItem.builder()
                        .faqId(f.getFaqId())
                        .question(f.getQuestion())
                        .answer(f.getAnswer())
                        .keywords(parseKeywords(f.getKeywords()))
                        .createdAt(f.getCreatedAt().toLocalDate())
                        .build())
                .collect(Collectors.toList());

        return FaqListResponse.builder()
                .faqList(items)
                .totalCount(faqPage.getTotalElements())
                .totalPages(faqPage.getTotalPages())
                .currentPage(page)
                .size(size)
                .build();
    }

    // FAQ 단건 상세 조회
    @Transactional(readOnly = true)
    public FaqDetailResponse getFaqDetail(Long faqId) {
        Faq faq = faqRepository.findByFaqIdAndIsActiveTrue(faqId)
                .orElseThrow(() -> new CustomException(ErrorCode.FAQ_NOT_FOUND));

        return FaqDetailResponse.builder()
                .faqId(faq.getFaqId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .keywords(parseKeywords(faq.getKeywords()))
                .qType(faq.getQType())
                .qaCnt(faq.getQaCnt())
                .createdAt(faq.getCreatedAt().toLocalDate())
                .build();
    }

    // FAQ 수정
    @Transactional
    public FaqListUpdateResponse updateFaq(Long faqId, FaqListUpdateRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Faq faq = faqRepository.findByFaqIdAndIsActiveTrue(faqId)
                .orElseThrow(() -> new CustomException(ErrorCode.FAQ_NOT_FOUND));

        String beforeQuestion = faq.getQuestion();
        String beforeAnswer = faq.getAnswer();

        String keywordsJson = null;
        if (request.getKeywords() != null) {
            try {
                keywordsJson = objectMapper.writeValueAsString(request.getKeywords());
            } catch (Exception e) {
                log.warn("키워드 직렬화 실패");
            }
        }

        faq.update(request.getQuestion(), request.getAnswer(), keywordsJson);

        FaqEditHistory history = FaqEditHistory.builder()
                .faq(faq)
                .editedBy(user)
                .beforeQuestion(beforeQuestion)
                .beforeAnswer(beforeAnswer)
                .afterQuestion(request.getQuestion())
                .afterAnswer(request.getAnswer())
                .editReason(null)
                .build();
        faqEditHistoryRepository.save(history);

        return FaqListUpdateResponse.builder()
                .faqId(faq.getFaqId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }

    // FAQ 삭제 (soft delete)
    @Transactional
    public void deleteFaq(Long faqId) {
        Faq faq = faqRepository.findByFaqIdAndIsActiveTrue(faqId)
                .orElseThrow(() -> new CustomException(ErrorCode.FAQ_NOT_FOUND));

        faq.deactivate();
        log.info("FAQ 삭제 완료: faqId={}", faqId);
    }

    // FAQ 수정 이력 조회
    @Transactional(readOnly = true)
    public FaqEditHistoryResponse getFaqHistory(Long faqId) {
        Faq faq = faqRepository.findByFaqIdAndIsActiveTrue(faqId)
                .orElseThrow(() -> new CustomException(ErrorCode.FAQ_NOT_FOUND));

        List<FaqEditHistoryResponse.HistoryItem> items =
                faqEditHistoryRepository.findByFaq_FaqIdOrderByCreatedAtDesc(faqId).stream()
                        .map(h -> FaqEditHistoryResponse.HistoryItem.builder()
                                .historyId(h.getHistoryId())
                                .analysisId(h.getAnalysisJob() != null
                                        ? h.getAnalysisJob().getAnalysisId() : null)
                                .beforeQuestion(h.getBeforeQuestion())
                                .beforeAnswer(h.getBeforeAnswer())
                                .afterQuestion(h.getAfterQuestion())
                                .afterAnswer(h.getAfterAnswer())
                                .editReason(h.getEditReason())
                                .editedBy(h.getEditedBy().getEmail())
                                .createdAt(h.getCreatedAt())
                                .build())
                        .collect(Collectors.toList());

        return FaqEditHistoryResponse.builder()
                .faqId(faq.getFaqId())
                .histories(items)
                .build();
    }

    // FAQ 후보 추천 조회
    @Transactional(readOnly = true)
    public FaqCandidateListResponse getFaqRecommendations(Long analysisId) {
        List<FaqCandidate> candidates = faqCandidateRepository
                .findByAnalysisJobAnalysisId(analysisId);

        List<FaqCandidateListResponse.FaqCandidateItem> items = candidates.stream()
                .map(this::buildFaqCandidateItem)
                .collect(Collectors.toList());

        return FaqCandidateListResponse.builder()
                .analysisId(analysisId)
                .recommendations(items)
                .build();
    }

    // FAQ 후보 반영
    @Transactional
    public FaqApplyResponse applyFaqCandidate(FaqApplyRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        FaqCandidate candidate = faqCandidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new CustomException(ErrorCode.FAQ_CANDIDATE_NOT_FOUND));

        ReviewStatus beforeStatus = candidate.getReviewStatus();

        candidate.accept(user);
        candidate.apply();

        String keywordsJson = null;
        if (request.getKeywords() != null) {
            try {
                keywordsJson = objectMapper.writeValueAsString(request.getKeywords());
            } catch (Exception e) {
                log.warn("키워드 직렬화 실패");
            }
        }

        Faq newFaq = Faq.builder()
                .candidate(candidate)
                .question(request.getFinalQuestion())
                .answer(request.getFinalAnswer())
                .keywords(keywordsJson)
                .createdBy(user)
                .build();
        faqRepository.save(newFaq);

        FaqActionLog actionLog = FaqActionLog.builder()
                .candidate(candidate)
                .actedBy(user)
                .action(FaqActionLog.Action.APPLY)
                .beforeStatus(beforeStatus)
                .afterStatus(ReviewStatus.APPLIED)
                .note(Boolean.TRUE.equals(request.getIsManualPatchConfirmed())
                        ? "수동 반영 확인됨" : null)
                .build();
        faqActionLogRepository.save(actionLog);

        log.info("FAQ 후보 반영 완료: candidateId={}, faqId={}",
                candidate.getCandidateId(), newFaq.getFaqId());

        return FaqApplyResponse.builder()
                .appliedFaqId(newFaq.getFaqId())
                .build();
    }

    private FaqCandidateListResponse.FaqCandidateItem buildFaqCandidateItem(FaqCandidate candidate) {
        List<FaqCandidateListResponse.FaqCandidateItem.SynonymItem> synonyms =
                synonymCandidateRepository
                        .findByCandidateCandidateId(candidate.getCandidateId())
                        .stream()
                        .map(s -> FaqCandidateListResponse.FaqCandidateItem.SynonymItem.builder()
                                .text(s.getSynonymText())
                                .type(s.getSynonymType())
                                .build())
                        .collect(Collectors.toList());

        return FaqCandidateListResponse.FaqCandidateItem.builder()
                .clusterId(candidate.getCluster() != null
                        ? candidate.getCluster().getClusterId() : null)
                .clusterLabel(candidate.getCluster() != null
                        ? candidate.getCluster().getClusterLabel() : null)
                .candidateId(candidate.getCandidateId())
                .candidateType(candidate.getCandidateType())
                .standardQuestion(candidate.getStandardQuestion())
                .answerDraft(candidate.getAnswerDraft())
                .reviewStatus(candidate.getReviewStatus())
                
                .representativeKeywords(candidate.getRepresentativeKeywords()) 
                
                .occurrenceCount(candidate.getOccurrenceCount())
                .synonyms(synonyms)
                .createdAt(candidate.getCreatedAt())
                .build();
    }

    private List<String> parseKeywords(String json) {
        if (json == null) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("키워드 파싱 실패: {}", json);
            return List.of();
        }
    }
}