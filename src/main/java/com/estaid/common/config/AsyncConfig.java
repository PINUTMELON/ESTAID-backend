package com.estaid.common.config;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 처리 스레드 풀 설정
 *
 * <p>이미지·영상 생성(@Async) 작업에 사용되는 커스텀 스레드 풀을 정의한다.
 * Spring 기본 SimpleAsyncTaskExecutor는 매 호출마다 새 스레드를 생성하므로,
 * 커스텀 풀로 스레드 재사용과 큐 제어를 보장한다.</p>
 *
 * <p>풀 크기 기준:</p>
 * <ul>
 *   <li>corePoolSize(4)  — 평상시 유지 스레드 (이미지+영상 동시 생성 2~3건 기준)</li>
 *   <li>maxPoolSize(8)   — 피크 시 확장 상한 (HikariCP 풀 15개 중 절반 이내)</li>
 *   <li>queueCapacity(50) — 대기 큐 (FAL.ai API 응답 지연 시 버퍼)</li>
 * </ul>
 */
@Slf4j
@Configuration
public class AsyncConfig {

    /**
     * @Async 기본 실행기 빈 (taskExecutor)
     *
     * <p>Spring은 'taskExecutor'라는 이름의 빈을 @Async 기본 Executor로 사용한다.</p>
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 스레드 수 — 평상시 유지
        executor.setCorePoolSize(4);

        // 최대 스레드 수 — 큐가 가득 찬 경우 확장
        executor.setMaxPoolSize(8);

        // 대기 큐 용량 — core 스레드가 모두 사용 중일 때 태스크 대기열
        executor.setQueueCapacity(50);

        // 스레드 이름 접두사 — 로그에서 비동기 작업 식별 용이
        executor.setThreadNamePrefix("async-fal-");

        // 유휴 스레드 제거 대기 시간 (초) — core 초과 스레드만 해당
        executor.setKeepAliveSeconds(60);

        // 애플리케이션 종료 시 진행 중인 태스크 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        log.info("비동기 스레드 풀 초기화 완료 (core={}, max={}, queue={})",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }
}
