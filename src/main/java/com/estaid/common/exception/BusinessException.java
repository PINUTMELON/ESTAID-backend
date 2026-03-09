package com.estaid.common.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

/**
 * 비즈니스 로직 예외
 * - 서비스 레이어에서 비즈니스 규칙 위반 시 던진다.
 * - GlobalExceptionHandler에서 catch하여 적절한 HTTP 응답으로 변환한다.
 *
 * 사용 예시:
 *   throw new BusinessException("캐릭터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }
}
