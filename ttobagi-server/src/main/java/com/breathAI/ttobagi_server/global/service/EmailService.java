package com.breathAI.ttobagi_server.global.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;

    @Value("${spring.mail.username:noreply@ttobagi.com}")
    private String fromEmail;

    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("[또바기] 비밀번호 재설정 안내입니다.");
        
        String content = String.format(
            "안녕하세요, 또바기 서비스입니다.\n\n" +
            "비밀번호 재설정을 위해 아래의 인증 토큰을 입력해 주세요.\n" +
            "토큰: %s\n\n" +
            "이 토큰은 30분 동안만 유효합니다.", 
            token
        );
        
        message.setText(content);

        emailSender.send(message);
    }
}