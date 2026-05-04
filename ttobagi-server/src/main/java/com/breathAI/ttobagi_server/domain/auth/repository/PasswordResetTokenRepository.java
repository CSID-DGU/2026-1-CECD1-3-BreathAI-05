package com.breathAI.ttobagi_server.domain.auth.repository;

import com.breathAI.ttobagi_server.domain.auth.entity.PasswordResetToken;
import com.breathAI.ttobagi_server.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.LocalDateTime;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(User user);

    void deleteByUser(User user);
    void deleteAllByExpiryDateBefore(LocalDateTime now);
}