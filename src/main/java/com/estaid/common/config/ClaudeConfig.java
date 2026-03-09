package com.estaid.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Claude API (Anthropic) WebClient 설정
 * - application.yml의 claude.api.* 값을 읽어 WebClient를 구성한다.
 * - 서비스에서 @Qualifier("claudeWebClient")로 주입받아 사용한다.
 *
 * 사용 예시:
 *   @Autowired
 *   @Qualifier("claudeWebClient")
 *   private WebClient claudeWebClient;
 *
 *   claudeWebClient.post()
 *       .uri("/v1/messages")
 *       .bodyValue(requestBody)
 *       .retrieve()
 *       .bodyToMono(String.class)
 *       .block();
 */
@Configuration
public class ClaudeConfig {

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.base-url}")
    private String baseUrl;

    @Value("${claude.api.version}")
    private String anthropicVersion;

    /**
     * Claude API 호출용 WebClient 빈
     * - Authorization 헤더: x-api-key
     * - Anthropic 버전 헤더: anthropic-version
     */
    @Bean("claudeWebClient")
    public WebClient claudeWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", anthropicVersion)
                .defaultHeader("content-type", "application/json")
                .build();
    }
}
