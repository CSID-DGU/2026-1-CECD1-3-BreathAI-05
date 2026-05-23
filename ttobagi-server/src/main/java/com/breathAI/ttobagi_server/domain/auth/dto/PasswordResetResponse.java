package com.breathAI.ttobagi_server.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetResponse {
    
    private String email;
}
