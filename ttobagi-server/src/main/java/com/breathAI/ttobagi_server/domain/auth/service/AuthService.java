package com.breathAI.ttobagi_server.domain.auth.service;

import com.breathAI.ttobagi_server.domain.auth.dto.*;
import com.breathAI.ttobagi_server.domain.auth.dto.UserInfoResponse;
import com.breathAI.ttobagi_server.domain.auth.dto.UserUpdateRequest;
import com.breathAI.ttobagi_server.domain.auth.entity.User;
import com.breathAI.ttobagi_server.domain.auth.entity.User.Role;
import com.breathAI.ttobagi_server.domain.auth.entity.PasswordResetToken;
import com.breathAI.ttobagi_server.domain.auth.repository.UserRepository;
import com.breathAI.ttobagi_server.domain.auth.repository.PasswordResetTokenRepository;
import com.breathAI.ttobagi_server.domain.dashboard.repository.UploadFileRepository;
import com.breathAI.ttobagi_server.domain.faq.repository.FaqActionLogRepository;    
import com.breathAI.ttobagi_server.domain.faq.repository.FaqCandidateRepository;
import com.breathAI.ttobagi_server.domain.faq.repository.FaqEditHistoryRepository;
import com.breathAI.ttobagi_server.domain.faq.repository.FaqRepository;
import com.breathAI.ttobagi_server.domain.faq.repository.RetrieveLogRepository;
import com.breathAI.ttobagi_server.global.exception.CustomException;
import com.breathAI.ttobagi_server.global.exception.ErrorCode;
import com.breathAI.ttobagi_server.global.util.JwtUtil;
import com.breathAI.ttobagi_server.global.service.EmailService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final UploadFileRepository uploadFileRepository;
    private final FaqActionLogRepository faqActionLogRepository;
    private final FaqCandidateRepository faqCandidateRepository;
    private final FaqEditHistoryRepository faqEditHistoryRepository;
    private final FaqRepository faqRepository;
    private final RetrieveLogRepository retrieveLogRepository;

    @Value("${system.admin-code}")
    private String systemAdminCode;

    @Transactional
    public void signUp(SignupRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
        }

        // 관리자 코드 검증 및 Role 결정
        Role role = (request.getAdminCode() != null && request.getAdminCode().equals(systemAdminCode))
                    ? Role.ADMIN : Role.USER;
        
        // entity 생성 및 저장
        User user = User.builder()
            .email(request.getEmail().toLowerCase())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(role)
            .build();
        
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        LocalDateTime accessTokenExpiresAt = jwtUtil.extractExpiration(accessToken)
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();

        LocalDateTime refreshTokenExpiresAt = jwtUtil.extractExpiration(refreshToken)
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        
            return LoginResponse.builder()
            .accessToken(accessToken)
            .accessTokenExpiresAt(accessTokenExpiresAt)
            .refreshToken(refreshToken)
            .refreshTokenExpiresAt(refreshTokenExpiresAt)
            .tokenType("Bearer")
            .build();
    }

    @Transactional
    public PasswordResetResponse resetPassword(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 기존 토큰 삭제
        passwordResetTokenRepository.deleteByUser(user);

        // 새 토큰 생성
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryMinutes(30)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // 메일 발송
        emailService.sendPasswordResetEmail(user.getEmail(), token);

        return new PasswordResetResponse(user.getEmail());
    }

    @Transactional
    public void confirmResetPassword(PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }
        
        if (resetToken.isUsed()) { 
            throw new CustomException(ErrorCode.ALREADY_USED_TOKEN);
        }

        User user = resetToken.getUser();
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));

        resetToken.useToken(); 
    }

    @Transactional
    public UserInfoResponse promoteUser(String email, PromoteRequest request) {
        // 유저 찾기
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 관리자 코드 검증
        if (request.getAdminCode() == null || !request.getAdminCode().equals(systemAdminCode)) {            throw new CustomException(ErrorCode.INVALID_ADMIN_CODE);
        }

        user.updateRole(Role.ADMIN);
        
        return getMyInfo(email);
    }
    
    public UserInfoResponse getMyInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UserInfoResponse updateMyInfo(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));

        return getMyInfo(email);
    }

    @Transactional
    public void deleteMyInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        uploadFileRepository.findByUploadedBy(user)
                .forEach(file -> file.clearUploadedBy());
        faqActionLogRepository.clearActedBy(user);
        faqCandidateRepository.clearReviewedBy(user);
        faqEditHistoryRepository.clearEditedBy(user);
        faqRepository.clearCreatedBy(user);
        retrieveLogRepository.clearUser(user);
        userRepository.delete(user);
    }

    @Transactional
    public TokenResponse reissue(TokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String email = jwtUtil.extractEmail(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        LocalDateTime newaccessTokenExpiresAt = jwtUtil.extractExpiration(newAccessToken)
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();

        LocalDateTime newrefreshTokenExpiresAt = jwtUtil.extractExpiration(newRefreshToken)
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresAt(newaccessTokenExpiresAt)
                .refreshTokenExpiresAt(newrefreshTokenExpiresAt)
                .tokenType("Bearer")
                .build();
    }
    
}
