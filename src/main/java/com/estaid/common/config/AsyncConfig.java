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
     * @Async 기본 실행기 빈 (taskExecutor) — 이미지 생성 우선
     *
     * <p>이미지 생성은 30~60초로 비교적 짧으므로 스레드를 넉넉히 배분한다.
     * 배치 생성(FIRST+LAST 동시) 시 2스레드를 동시 사용하므로 core=6으로 설정.</p>
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-image-");
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("이미지 스레드 풀 초기화 완료 (core={}, max={}, queue={})",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }

    /**
     * 영상 생성 전용 실행기 빈 (videoExecutor)
     *
     * <p>영상 생성은 1~10분으로 오래 걸리며 폴링으로 스레드를 점유하므로,
     * 이미지 생성과 분리하여 영상이 이미지 생성을 블로킹하지 않도록 한다.</p>
     */
    @Bean(name = "videoExecutor")
    public Executor videoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("async-video-");
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("영상 스레드 풀 초기화 완료 (core={}, max={}, queue={})",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }
}
