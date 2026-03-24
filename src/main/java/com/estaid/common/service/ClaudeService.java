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
     * 스토리 설명을 기반으로 Claude API를 호출하여 씬 목록을 생성한다.
     *
     * <p>호출 흐름:</p>
     * <pre>
     *   1. 씬 생성 프롬프트 구성 (스토리 설명, 씬 수, 비율)
     *   2. Claude API POST /v1/messages 동기 호출
     *   3. 응답 텍스트에서 JSON 배열 추출
     *   4. List&lt;SceneDto&gt;로 역직렬화하여 반환
     * </pre>
     *
     * @param storyDescription 사용자가 입력한 전체 줄거리 텍스트
     * @param sceneCount       생성할 씬 수 (1~10)
     * @param ratio            영상 비율 (예: "16:9", null 허용)
     * @return Claude가 생성한 씬 목록
     * @throws BusinessException Claude API 호출 실패 또는 응답 파싱 실패 시
     */
    public List<SceneDto> generateScenes(String storyDescription, int sceneCount, String ratio) {
        return generateScenes(storyDescription, sceneCount, ratio, null, null);
    }

    /**
     * 스토리 설명 + 캐릭터 정보를 기반으로 Claude API를 호출하여 씬 목록을 생성한다.
     *
     * <p>캐릭터 이름/설명이 제공되면, Claude가 firstFramePrompt/lastFramePrompt를
     * 생성할 때 캐릭터 외형을 구체적으로 반영하도록 프롬프트에 포함한다.</p>
     *
     * @param storyDescription    사용자가 입력한 전체 줄거리 텍스트
     * @param sceneCount          생성할 씬 수 (1~10)
     * @param ratio               영상 비율 (예: "16:9", null 허용)
     * @param characterName       캐릭터 이름 (null 허용)
     * @param characterDescription 캐릭터 외형/특징 설명 (null 허용)
     * @return Claude가 생성한 씬 목록
     * @throws BusinessException Claude API 호출 실패 또는 응답 파싱 실패 시
     */
    public List<SceneDto> generateScenes(String storyDescription, int sceneCount, String ratio,
                                          String characterName, String characterDescription) {
        log.info("Claude 씬 생성 요청: sceneCount={}, ratio={}, characterName={}", sceneCount, ratio, characterName);

        // 1. 프롬프트 구성 (캐릭터 정보 포함)
        String userPrompt = buildScenePrompt(storyDescription, sceneCount, ratio, characterName, characterDescription);

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
     * storyDescription과 씬 수, 비율을 기반으로 Claude에게 전달할 프롬프트를 만든다.
     *
     * @param storyDescription 전체 줄거리 텍스트
     * @param sceneCount       생성할 씬 수
     * @param ratio            영상 비율 (null 허용)
     */
    /**
     * 씬 생성용 사용자 프롬프트를 구성한다.
     * storyDescription과 씬 수, 비율, 캐릭터 정보를 기반으로 Claude에게 전달할 프롬프트를 만든다.
     *
     * @param storyDescription    전체 줄거리 텍스트
     * @param sceneCount          생성할 씬 수
     * @param ratio               영상 비율 (null 허용)
     * @param characterName       캐릭터 이름 (null 허용)
     * @param characterDescription 캐릭터 외형/특징 설명 (null 허용)
     */
    private String buildScenePrompt(String storyDescription, int sceneCount, String ratio,
                                     String characterName, String characterDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append("줄거리: ").append(storyDescription).append("\n");
        sb.append("씬 수: ").append(sceneCount).append("개\n");

        if (ratio != null && !ratio.isBlank()) {
            sb.append("영상 비율: ").append(ratio).append("\n");
        }

        // 캐릭터 정보가 있으면 프롬프트에 포함하여 씬 프롬프트에 외형이 반영되도록 한다
        if (characterName != null && !characterName.isBlank()) {
            sb.append("\n[캐릭터 정보]\n");
            sb.append("캐릭터 이름: ").append(characterName).append("\n");
            if (characterDescription != null && !characterDescription.isBlank()) {
                sb.append("캐릭터 외형/특징: ").append(characterDescription).append("\n");
            }
            sb.append("※ 위 캐릭터의 외형을 모든 씬의 firstFramePrompt/lastFramePrompt에 구체적으로 반영하세요.\n");
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
            "사용자의 줄거리를 받아 지정된 수의 씬으로 구성된 스토리보드를 JSON 형식으로 생성하세요.\n" +
            "각 씬은 영상 제작에 필요한 구체적인 정보를 포함해야 합니다.\n" +
            "composition 필드는 반드시 아래 10가지 값 중 하나만 사용하세요:\n" +
            "EXTREME_CLOSEUP, HIGH_ANGLE, LOW_ANGLE, MEDIUM_SHOT, OVER_THE_SHOULDER, " +
            "TWO_SHOT, WIDE_SHOT, BIRD_EYE_VIEW, CLOSEUP, DUTCH_ANGLE\n" +
            "반드시 JSON 배열만 반환하고, 다른 텍스트나 설명은 절대 포함하지 마세요.";

    /**
     * 영상 프롬프트 생성용 사용자 프롬프트를 구성한다.
     * 씬의 모든 시각적 정보를 포함하여 일관된 영상 프롬프트를 생성한다.
     *
     * @param scene    씬 정보 DTO
     * @param artStyle 화풍 설정 (null 허용)
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
        // backgroundDetail이 있으면 배경 상세 묘사를 추가로 전달한다
        if (scene.getBackgroundDetail() != null && !scene.getBackgroundDetail().isBlank()) {
            sb.append("배경 상세: ").append(scene.getBackgroundDetail()).append("\n");
        }
        sb.append("조명/분위기: ").append(scene.getLighting()).append("\n");
        // majorStory가 있으면 우선 사용, 없으면 빈 값
        String story = scene.getMajorStory();
        sb.append("주요 스토리: ").append(story != null ? story : "").append("\n");
        sb.append("첫 프레임 묘사: ").append(scene.getFirstFramePrompt()).append("\n");
        sb.append("마지막 프레임 묘사: ").append(scene.getLastFramePrompt()).append("\n");
        if (artStyle != null && !artStyle.isBlank()) {
            sb.append("화풍: ").append(artStyle).append("\n");
        }
        return sb.toString();
    }

    /** 영상 프롬프트 생성 시스템 프롬프트 */
    private static final String VIDEO_PROMPT_SYSTEM =
            "You are an expert at writing video generation prompts for FAL.ai Wan 2.1 FLF2V model.\n" +
            "This model interpolates between a START frame and an END frame to create a smooth video.\n" +
            "Your task: Write a single, concise English prompt (2-3 sentences) that describes:\n" +
            "  1. The MOTION and TRANSITION happening between the two frames (most important)\n" +
            "  2. Camera movement (e.g., slow pan, gentle zoom-in, static shot, dolly forward)\n" +
            "  3. Character actions and how they move or change during the scene\n" +
            "Focus on MOVEMENT and DYNAMICS, NOT static description of the scene.\n" +
            "Do NOT describe what is in the scene — describe how it MOVES.\n" +
            "Return ONLY the prompt text with no explanation or commentary.";

    /**
     * 씬 JSON 형식 예시 (Claude에게 출력 포맷 안내)
     *
     * <p>주요 필드 변경 사항:</p>
     * <ul>
     *   <li>composition: 반드시 Enum 10가지 중 하나 (EXTREME_CLOSEUP 등)</li>
     *   <li>background: 배경 장소 이름 (1~3단어)</li>
     *   <li>backgroundDetail: 배경 상세 묘사 (영어, 이미지 생성용)</li>
     *   <li>majorStory: 씬 구체적 설명 (기존 mainStory 대체)</li>
     * </ul>
     */
    private static final String SCENE_JSON_FORMAT =
            "[\n" +
            "  {\n" +
            "    \"sceneNumber\": 1,\n" +
            "    \"title\": \"씬 제목\",\n" +
            "    \"characters\": \"등장인물\",\n" +
            "    \"composition\": \"반드시 다음 중 하나: EXTREME_CLOSEUP, HIGH_ANGLE, LOW_ANGLE, MEDIUM_SHOT, OVER_THE_SHOULDER, TWO_SHOT, WIDE_SHOT, BIRD_EYE_VIEW, CLOSEUP, DUTCH_ANGLE\",\n" +
            "    \"background\": \"배경 장소 이름 (1~3단어, 예: 어두운 숲)\",\n" +
            "    \"backgroundDetail\": \"배경 상세 묘사 (영어, 이미지 생성 프롬프트용, 예: Dense pine forest with moonlight)\",\n" +
            "    \"lighting\": \"조명/분위기 (영어 권장)\",\n" +
            "    \"majorStory\": \"씬에 대한 구체적 설명/사건 (2~3문장)\",\n" +
            "    \"firstFramePrompt\": \"영어 이미지 생성 프롬프트 (첫 프레임)\",\n" +
            "    \"lastFramePrompt\": \"영어 이미지 생성 프롬프트 (마지막 프레임)\"\n" +
            "  }\n" +
            "]";
}
