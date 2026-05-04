package com.breathAI.ttobagi_server.domain.auth.controller;

import com.breathAI.ttobagi_server.domain.auth.dto.SignupRequest;
import com.breathAI.ttobagi_server.global.dto.ApiResponse;
import com.breathAI.ttobagi_server.domain.auth.dto.LoginRequest;
import com.breathAI.ttobagi_server.domain.auth.dto.LoginResponse;
import com.breathAI.ttobagi_server.domain.auth.dto.PasswordResetResponse;
import com.breathAI.ttobagi_server.domain.auth.dto.PasswordResetRequest;
import com.breathAI.ttobagi_server.domain.auth.dto.PasswordResetConfirmRequest;
import com.breathAI.ttobagi_server.domain.auth.dto.PromoteRequest;
import com.breathAI.ttobagi_server.domain.auth.dto.UserInfoResponse;
import com.breathAI.ttobagi_server.domain.auth.dto.UserUpdateRequest;
import com.breathAI.ttobagi_server.domain.auth.dto.TokenRequest;
import com.breathAI.ttobagi_server.domain.auth.dto.TokenResponse;
import com.breathAI.ttobagi_server.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody @Valid SignupRequest request) {
        authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<PasswordResetResponse>> resetPassword(
            @RequestBody @Valid PasswordResetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.resetPassword(request)));
    }

    @PostMapping("/password/reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPassword(
            @RequestBody @Valid PasswordResetConfirmRequest request) {
        authService.confirmResetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMyInfo(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.success(authService.getMyInfo(email)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateMyInfo(
            @AuthenticationPrincipal String email,
            @RequestBody @Valid UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.updateMyInfo(email, request)));
    }

    @PatchMapping("/promote")
    public ResponseEntity<ApiResponse<UserInfoResponse>> promoteUser(
            @AuthenticationPrincipal String email,
            @RequestBody @Valid PromoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.promoteUser(email, request)));
    }

    @DeleteMapping("/quit")
    public ResponseEntity<ApiResponse<Void>> deleteMyInfo(
            @AuthenticationPrincipal String email) {
        authService.deleteMyInfo(email);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@RequestBody TokenRequest request) {
        TokenResponse response = authService.reissue(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
