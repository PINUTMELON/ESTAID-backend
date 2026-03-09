package com.estaid.common.response;

import lombok.Getter;

/**
 * 표준 API 응답 래퍼
 *
 * 모든 API는 이 형식으로 응답을 반환한다:
 * {
 *   "success": true,
 *   "message": "요청 성공",
 *   "data": { ... }
 * }
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /** 성공 응답 (데이터 포함) */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "요청 성공", data);
    }

    /** 성공 응답 (메시지 + 데이터) */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /** 실패 응답 */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
