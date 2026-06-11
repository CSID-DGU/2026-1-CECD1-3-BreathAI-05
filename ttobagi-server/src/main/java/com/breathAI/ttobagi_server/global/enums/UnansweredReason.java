package com.breathAI.ttobagi_server.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UnansweredReason {
    UNREGISTERED_KEYWORD("키워드 미등록"),
    FOREIGN_LANGUAGE("외국어 질의"),
    POLICY_RESTRICTION("보안/정책상 답변 불가"),
    ETC("기타");

    private final String description;

    public static UnansweredReason fromDescription(String description) {
        for (UnansweredReason reason : values()) {
            if (reason.description.equals(description)) {
                return reason;
            }
        }
        return ETC;
    }
}