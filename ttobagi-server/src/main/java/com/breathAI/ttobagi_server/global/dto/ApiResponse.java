package com.breathAI.ttobagi_server.global.dto;

import com.breathAI.ttobagi_server.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private int status;

    // 생성자
    public ApiResponse(boolean success, String message, T data, int status) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.status = status;
    }

    // 성공 응답
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "요청에 성공하였습니다.", data, 200);
    }

    // Void 응답
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, "요청에 성공하였습니다.", null, 200);
    }

    // 커스텀 메시지
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, 200);
    }

    // 에러 응답 메서드
    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getMessage(), null, errorCode.getStatus());
    }

    // 커스텀 메시지 필요한 경우
    public static ApiResponse<Void> error(String message, int status) {
        return new ApiResponse<>(false, message, null, status);
    }
}