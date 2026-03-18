package com.estaid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ESTAID 애플리케이션 진입점
 *
 * <p>{@code @EnableAsync}: 이미지·영상 생성 비동기 처리(@Async)를 활성화한다.
 * FAL.ai API 호출은 오래 걸리므로 요청 스레드를 차단하지 않고 별도 스레드에서 처리한다.</p>
 */
@SpringBootApplication
@EnableAsync
public class EstaidApplication {
    public static void main(String[] args) {
        SpringApplication.run(EstaidApplication.class, args);
    }
}
