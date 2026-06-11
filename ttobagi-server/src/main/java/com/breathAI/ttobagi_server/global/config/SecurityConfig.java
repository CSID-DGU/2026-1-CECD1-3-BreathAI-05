package com.breathAI.ttobagi_server.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * 개발 단계용 Security 설정.
     *
     * 현재 목적:
     * - Spring Boot에서 분석 요청 API 테스트
     * - AI 서버 callback 수신 테스트
     * - Swagger 접근 허용
     *
     * 추후 실제 인증/인가 구조가 확정되면
     * permitAll 범위를 줄이고 JWT/Spring Security 인증 정책을 적용해야 한다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // AI 서버 callback, multipart 분석 요청 테스트를 위해 CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 개발 단계에서는 form login/basic auth 사용하지 않음
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // REST API 서버이므로 세션을 생성하지 않음
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        // CORS preflight 요청 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 분석 요청 및 AI callback endpoint 허용
                        .requestMatchers("/api/v1/dashboard/analyze/**").permitAll()

                        // Swagger/OpenAPI 허용
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 기본 health 확인용 endpoint가 생길 경우 허용
                        .requestMatchers(
                                "/actuator/**",
                                "/health"
                        ).permitAll()

                        // 개발 단계에서는 나머지도 임시 허용
                        // 추후 인증 구조가 붙으면 authenticated()로 변경
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}