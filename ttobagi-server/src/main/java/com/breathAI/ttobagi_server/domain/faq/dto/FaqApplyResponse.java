package com.breathAI.ttobagi_server.domain.faq.dto;

import lombok.*;
import jakarta.validation.constraints.NotNull;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqApplyResponse {
    @NotNull
    private Long appliedFaqId;
}