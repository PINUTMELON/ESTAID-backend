package com.estaid.common.service;

import com.estaid.character.Character;
import com.estaid.common.exception.BusinessException;
import com.estaid.plot.dto.SceneDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Claude API 연동 서비스
 *
 * <p>Anthropic Claude API를 호출하여 다음 기능을 제공한다:</p>
 * <ul>
 *   <li>{@link #generateScenes}      - 플롯 아이디어 → N개의 씬 JSON 자동 생성</li>
 *   <li>{@link #generateVideoPrompt} - 씬 정보 → 영상 생성 프롬프트 자동 생성</li>
 * </ul>
 *
 * <p>호출 방식: 동기({@code .block()}), 타임아웃 60초</p>
 * <p>Claude API 응답에서 JSON 배열을 추출하여 {@code List<SceneDto>}로 역직렬화한다.
 * 마크다운 코드블록(```json ... ```)으로 감싸진 응답도 처리한다.</p>
 */
@Slf4j
@Service
public class ClaudeService {

    /** Claude API 호출용 WebClient (ClaudeConfig에서 주입) */
    private final WebClient claudeWebClient;

    /** Jackson ObjectMapper (JSON 직렬화·역직렬화) */
    private final ObjectMapper objectMapper;

    /** 사용할 Claude 모델명 (application.yml: claude.api.model) */
    @Value("${claude.api.model}")
    private String model;

    /** Claude API 호출 타임아웃 (초) */
    private static final int TIMEOUT_SECONDS = 60;

    /**
     * 생성자 주입
     * WebClient는 @Qualifier로 "claudeWebClient" 빈을 명시적으로 지정한다.
     */
    public ClaudeService(@Qualifier("claudeWebClient") WebClient claudeWebClient,
                         ObjectMapper objectMapper) {
        this.claudeWebClient = claudeWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 플롯 아이디어를 기반으로 Claude API를 호출하여 씬 목록을 생성한다.
     *
     * <p>호출 흐름:</p>
     * <pre>
     *   1. 씬 생성 프롬프트 구성 (제목, 아이디어, 씬 수, 아트스타일, 캐릭터 정보)
     *   2. Claude API POST /v1/messages 동기 호출
     *   3. 응답 텍스트에서 JSON 배열 추출
     *   4. List&lt;SceneDto&gt;로 역직렬화하여 반환
     * </pre>
     *
     * @param title      플롯 제목
     * @param idea       사용자가 입력한 스토리 아이디어
     * @param sceneCount 생성할 씬 수 (1~10)
     * @param artStyle   화풍 설정 (예: anime, realistic)
     * @param character  참조 캐릭터 (null 허용 — 없으면 캐릭터 정보 제외)
     * @return Claude가 생성한 씬 목록
     * @throws BusinessException Claude API 호출 실패 또는 응답 파싱 실패 시
     */
    public List<SceneDto> generateScenes(String title, String idea, int sceneCount,
                                         String artStyle, Character character) {
        log.info("Claude 씬 생성 요청: title={}, sceneCount={}, artStyle={}", title, sceneCount, artStyle);

        // 1. 프롬프트 구성
        String userPrompt = buildScenePrompt(title, idea, sceneCount, artStyle, character);

        // 2. Claude API 요청 바디 구성
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 4096,
                "system", SCENE_SYSTEM_PROMPT,
                "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );

        // 3. Claude API 동기 호출
        String responseJson;
        try {
            responseJson = claudeWebClient.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new BusinessException("Claude API 오류: " + body, HttpStatus.BAD_GATEWAY))))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Claude API HTTP 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("Claude API 호출에 실패했습니다.", HttpStatus.BAD_GATEWAY);
        } catch (Exception e) {
            log.error("Claude API 호출 실패: {}", e.getMessage());
            throw new BusinessException("Claude API 호출에 실패했습니다.", HttpStatus.BAD_GATEWAY);
        }

        // 4. 응답 파싱 → List<SceneDto>
        List<SceneDto> scenes = parseScenes(responseJson);
        log.info("Claude 씬 생성 완료: 생성된 씬 수={}", scenes.size());
        return scenes;
    }

    /**
     * 씬 정보를 기반으로 Claude API를 호출하여 영상 생성 프롬프트를 생성한다.
     *
     * <p>호출 흐름:</p>
     * <pre>
     *   1. 씬 정보 + 아트스타일로 프롬프트 요청 구성
     *   2. Claude API POST /v1/messages 동기 호출
     *   3. 응답 텍스트를 그대로 반환 (JSON 아님, 자연어 프롬프트)
     * </pre>
     *
     * @param scene    씬 정보 DTO (sceneNumber, characters, composition, background, lighting, mainStory)
     * @param artStyle 화풍 설정 (예: anime, realistic)
     * @return Claude가 생성한 영상 프롬프트 문자열
     * @throws BusinessException Claude API 호출 실패 시
     */
    public String generateVideoPrompt(SceneDto scene, String artStyle) {
        log.info("Claude 영상 프롬프트 생성 요청: sceneNumber={}, artStyle={}", scene.getSceneNumber(), artStyle);

        // 1. 프롬프트 구성
        String userPrompt = buildVideoPromptRequest(scene, artStyle);

        // 2. Claude API 요청 바디 구성
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 512,
                "system", VIDEO_PROMPT_SYSTEM,
                "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );

        // 3. Claude API 동기 호출
        String responseJson;
        try {
            responseJson = claudeWebClient.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new BusinessException("Claude API 오류: " + body, HttpStatus.BAD_GATEWAY))))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Claude API HTTP 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("Claude API 호출에 실패했습니다.", HttpStatus.BAD_GATEWAY);
        } catch (Exception e) {
            log.error("Claude API 호출 실패: {}", e.getMessage());
            throw new BusinessException("Claude API 호출에 실패했습니다.", HttpStatus.BAD_GATEWAY);
        }

        // 4. content[0].text 추출
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String prompt = root.path("content").get(0).path("text").asText().trim();
            log.info("Claude 영상 프롬프트 생성 완료: sceneNumber={}", scene.getSceneNumber());
            return prompt;
        } catch (Exception e) {
            log.error("Claude 영상 프롬프트 응답 파싱 실패: {}", e.getMessage());
            throw new BusinessException("Claude 영상 프롬프트 응답 파싱에 실패했습니다.");
        }
    }

    // ─────────────────────────────────────────
    // private 헬퍼
    // ─────────────────────────────────────────

    /**
     * 씬 생성용 사용자 프롬프트를 구성한다.
     * 캐릭터가 있으면 이름·설명을 포함하여 씬 내 캐릭터 묘사 일관성을 높인다.
     */
    private String buildScenePrompt(String title, String idea, int sceneCount,
                                    String artStyle, Character character) {
        StringBuilder sb = new StringBuilder();
        sb.append("제목: ").append(title).append("\n");
        sb.append("아이디어: ").append(idea).append("\n");
        sb.append("씬 수: ").append(sceneCount).append("개\n");

        if (artStyle != null && !artStyle.isBlank()) {
            sb.append("아트 스타일: ").append(artStyle).append("\n");
        }
        if (character != null) {
            sb.append("캐릭터: ").append(character.getName());
            if (character.getDescription() != null && !character.getDescription().isBlank()) {
                sb.append(" - ").append(character.getDescription());
            }
            sb.append("\n");
        }

        sb.append("\n다음 JSON 형식으로 정확히 ").append(sceneCount).append("개의 씬을 생성해주세요.\n");
        sb.append("JSON 배열만 반환하고 다른 설명은 절대 추가하지 마세요:\n");
        sb.append(SCENE_JSON_FORMAT);

        return sb.toString();
    }

    /**
     * Claude API 응답 JSON에서 씬 배열을 파싱하여 {@code List<SceneDto>}로 반환한다.
     *
     * <p>처리 순서:</p>
     * <ol>
     *   <li>응답 JSON에서 {@code content[0].text} 추출</li>
     *   <li>마크다운 코드블록(```json...```) 제거</li>
     *   <li>텍스트에서 {@code [ ... ]} JSON 배열 부분 추출</li>
     *   <li>Jackson으로 {@code List<SceneDto>} 역직렬화</li>
     * </ol>
     */
    private List<SceneDto> parseScenes(String responseJson) {
        try {
            // content[0].text 추출
            JsonNode root = objectMapper.readTree(responseJson);
            String text = root.path("content").get(0).path("text").asText();

            // 마크다운 코드블록 제거
            text = text.replaceAll("(?s)```json\\s*", "")
                       .replaceAll("(?s)```\\s*", "")
                       .trim();

            // JSON 배열 범위 추출
            int start = text.indexOf('[');
            int end   = text.lastIndexOf(']');
            if (start == -1 || end == -1 || start >= end) {
                log.error("Claude 응답에서 JSON 배열을 찾을 수 없음: {}", text);
                throw new BusinessException("Claude 응답에서 씬 목록을 파싱할 수 없습니다.");
            }
            String jsonArray = text.substring(start, end + 1);

            return objectMapper.readValue(jsonArray, new TypeReference<List<SceneDto>>() {});
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Claude 응답 파싱 실패: {}", e.getMessage());
            throw new BusinessException("Claude 응답 파싱에 실패했습니다. 다시 시도해주세요.");
        }
    }

    // ─────────────────────────────────────────
    // 프롬프트 상수
    // ─────────────────────────────────────────

    /** 씬 생성 시스템 프롬프트 */
    private static final String SCENE_SYSTEM_PROMPT =
            "당신은 2차 창작 만화/영상의 스토리보드 작가입니다.\n" +
            "사용자의 아이디어를 받아 지정된 수의 씬으로 구성된 플롯을 JSON 형식으로 생성하세요.\n" +
            "각 씬은 영상 제작에 필요한 구체적인 정보를 포함해야 합니다.\n" +
            "반드시 JSON 배열만 반환하고, 다른 텍스트나 설명은 절대 포함하지 마세요.";

    /**
     * 영상 프롬프트 생성용 사용자 프롬프트를 구성한다.
     * 씬의 모든 시각적 정보를 포함하여 일관된 영상 프롬프트를 생성한다.
     */
    private String buildVideoPromptRequest(SceneDto scene, String artStyle) {
        StringBuilder sb = new StringBuilder();
        sb.append("씬 번호: ").append(scene.getSceneNumber()).append("\n");
        if (scene.getTitle() != null && !scene.getTitle().isBlank()) {
            sb.append("씬 제목: ").append(scene.getTitle()).append("\n");
        }
        sb.append("등장인물: ").append(scene.getCharacters()).append("\n");
        sb.append("카메라 구도: ").append(scene.getComposition()).append("\n");
        sb.append("배경: ").append(scene.getBackground()).append("\n");
        sb.append("조명/분위기: ").append(scene.getLighting()).append("\n");
        sb.append("주요 스토리: ").append(scene.getMainStory()).append("\n");
        sb.append("첫 프레임 묘사: ").append(scene.getFirstFramePrompt()).append("\n");
        sb.append("마지막 프레임 묘사: ").append(scene.getLastFramePrompt()).append("\n");
        if (artStyle != null && !artStyle.isBlank()) {
            sb.append("화풍: ").append(artStyle).append("\n");
        }
        return sb.toString();
    }

    /** 영상 프롬프트 생성 시스템 프롬프트 */
    private static final String VIDEO_PROMPT_SYSTEM =
            "당신은 AI 영상 생성 전문가입니다.\n" +
            "씬 정보를 받아 FAL.ai Wan 2.1 모델에 최적화된 영상 생성 프롬프트를 영어로 작성하세요.\n" +
            "프롬프트는 시각적 동작, 카메라 움직임, 분위기를 구체적으로 묘사해야 합니다.\n" +
            "반드시 영어로만 작성하고, 설명이나 부연 없이 프롬프트 텍스트만 반환하세요.\n" +
            "길이는 2~3문장으로 작성하세요.";

    /** 씬 JSON 형식 예시 (Claude에게 출력 포맷 안내) */
    private static final String SCENE_JSON_FORMAT =
            "[\n" +
            "  {\n" +
            "    \"sceneNumber\": 1,\n" +
            "    \"title\": \"씬 제목\",\n" +
            "    \"characters\": \"등장인물\",\n" +
            "    \"composition\": \"카메라 구도 (예: wide shot, close-up, low angle)\",\n" +
            "    \"background\": \"배경 묘사\",\n" +
            "    \"lighting\": \"조명/분위기\",\n" +
            "    \"mainStory\": \"주요 스토리 (2~3문장)\",\n" +
            "    \"firstFramePrompt\": \"영어 이미지 생성 프롬프트 (첫 프레임)\",\n" +
            "    \"lastFramePrompt\": \"영어 이미지 생성 프롬프트 (마지막 프레임)\"\n" +
            "  }\n" +
            "]";
}
