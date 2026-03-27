package com.estaid.common.service;

import com.estaid.image.Image;
import com.estaid.image.ImageRepository;
import com.estaid.project.Project;
import com.estaid.project.ProjectRepository;
import com.estaid.video.Video;
import com.estaid.video.VideoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * FAL.ai API 연동 서비스
 *
 * <p>다음 두 가지 FAL.ai 모델을 연동한다:</p>
 * <ul>
 *   <li>FLUX Kontext [pro] - 씬 이미지 생성 (동기, {@code fal.api.image-url})</li>
 *   <li>Wan 2.1 FLF2V     - 씬 영상 생성 (비동기 큐, {@code fal.api.video-url})</li>
 * </ul>
 *
 * <p>이미지 생성 흐름:</p>
 * <pre>
 *   POST {image-url} → { "images": [{ "url": "..." }] }  ← 동기 반환
 * </pre>
 *
 * <p>영상 생성 흐름:</p>
 * <pre>
 *   1. POST {video-url}                           → { "request_id": "..." }
 *   2. GET  {video-url}/requests/{id}/status      → { "status": "COMPLETED" }
 *   3. GET  {video-url}/requests/{id}             → { "video": { "url": "..." } }
 * </pre>
 *
 * <p>{@link #processImageGeneration}·{@link #processVideoGeneration}은 {@code @Async}로
 * 별도 스레드에서 실행되며, 완료 후 DB 상태(status, url)를 직접 갱신한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FalAiService {

    /** 이미지 엔티티 Repository (비동기 완료 후 상태·URL 갱신용) */
    private final ImageRepository imageRepository;

    /** 프로젝트 엔티티 Repository (이미지 생성 완료 시 썸네일 자동 설정용) */
    private final ProjectRepository projectRepository;

    /** 영상 엔티티 Repository (비동기 완료 후 상태·URL 갱신용) */
    private final VideoRepository videoRepository;

    /** JSON 직렬화·역직렬화 */
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────
    // application.yml 설정값
    // ─────────────────────────────────────────

    /** FAL.ai API 인증 키 (Authorization: Key {falApiKey}) */
    @Value("${fal.api.key}")
    private String falApiKey;

    /** FLUX Kontext 이미지 생성 엔드포인트 전체 URL (image-to-image, referenceImageUrl 필수) */
    @Value("${fal.api.image-url}")
    private String imageApiUrl;

    /** FLUX Pro 텍스트 이미지 생성 엔드포인트 전체 URL (text-to-image, referenceImageUrl 불필요) */
    @Value("${fal.api.text-image-url}")
    private String textImageApiUrl;

    /** Wan 2.1 FLF2V 영상 생성 큐 기본 URL */
    @Value("${fal.api.video-url}")
    private String videoApiUrl;

    /** FAL.ai 호출용 WebClient (PostConstruct에서 초기화) */
    private WebClient falWebClient;

    /** 영상 상태 폴링 초기 간격 (밀리초) — 지수 백오프 시작값 */
    private static final int POLLING_INITIAL_INTERVAL_MS = 2_000;

    /** 영상 상태 폴링 최대 간격 (밀리초) — 지수 백오프 상한 */
    private static final int POLLING_MAX_INTERVAL_MS = 15_000;

    /** 최대 폴링 총 시간 (밀리초) — 약 10분 */
    private static final long POLLING_TIMEOUT_MS = 600_000;

    /** FAL.ai API 호출 타임아웃 (초) — FLUX Kontext 느릴 때 대비 240초 */
    private static final int TIMEOUT_SECONDS = 240;

    /**
     * 빈 초기화 후 FAL.ai WebClient를 구성한다.
     * {@code @Value} 필드 주입 완료 후 실행되므로 {@code falApiKey}를 안전하게 사용할 수 있다.
     */
    @PostConstruct
    private void initWebClient() {
        this.falWebClient = WebClient.builder()
                .defaultHeader("Authorization", "Key " + falApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        log.info("FAL.ai WebClient 초기화 완료");
    }

    // ─────────────────────────────────────────
    // 이미지 생성 (비동기)
    // ─────────────────────────────────────────

    /**
     * 씬 이미지 비동기 생성
     *
     * <p>처리 흐름:</p>
     * <pre>
     *   1. Image 상태 → PROCESSING
     *   2. FAL.ai FLUX Kontext 호출 (referenceImageUrl + prompt)
     *   3. 성공 시 Image.imageUrl 저장, 상태 → COMPLETED
     *   4. 실패 시 상태 → FAILED
     * </pre>
     *
     * <p>{@code @Async}로 별도 스레드 풀에서 실행되므로 호출 즉시 반환된다.</p>
     *
     * @param imageId          생성할 Image 엔티티의 ID
     * @param referenceImageUrl 캐릭터 레퍼런스 이미지 URL (null 허용 - 없으면 프롬프트만 사용)
     * @param prompt           이미지 생성 프롬프트
     */
    @Async
    public void processImageGeneration(String imageId, String referenceImageUrl, String prompt) {
        log.info("이미지 비동기 생성 시작: imageId={}", imageId);

        // 1. 상태 → PROCESSING
        updateImageStatus(imageId, Image.GenerationStatus.PROCESSING, null);

        try {
            // 2. FAL.ai 이미지 생성 API 호출
            String generatedImageUrl = callImageApi(referenceImageUrl, prompt, null);

            // 3. 성공 → 상태 COMPLETED, imageUrl 저장
            updateImageStatus(imageId, Image.GenerationStatus.COMPLETED, generatedImageUrl);
            log.info("이미지 생성 완료: imageId={}, url={}", imageId, generatedImageUrl);

        } catch (Exception e) {
            // 4. 실패 → 상태 FAILED
            log.error("이미지 생성 실패: imageId={}, error={}", imageId, e.getMessage());
            updateImageStatus(imageId, Image.GenerationStatus.FAILED, null);
        }
    }

    // ─────────────────────────────────────────
    // 영상 생성 (비동기)
    // ─────────────────────────────────────────

    /**
     * 씬 영상 비동기 생성
     *
     * <p>처리 흐름:</p>
     * <pre>
     *   1. Video 상태 → PROCESSING
     *   2. FAL.ai 큐에 작업 제출 → requestId 획득
     *   3. 상태 폴링 (지수 백오프: 2초→4초→8초→15초 상한, 최대 10분)
     *   4. 성공 시 Video.videoUrl 저장, 상태 → COMPLETED
     *   5. 실패·타임아웃 시 상태 → FAILED
     * </pre>
     *
     * <p>{@code @Async}로 별도 스레드 풀에서 실행되므로 호출 즉시 반환된다.</p>
     *
     * @param videoId       생성할 Video 엔티티의 ID
     * @param firstFrameUrl 씬 첫 프레임 이미지 URL
     * @param lastFrameUrl  씬 마지막 프레임 이미지 URL
     * @param prompt        영상 생성 프롬프트 (Claude가 생성하거나 사용자가 수정한 값)
     */
    @Async
    public void processVideoGeneration(String videoId, String firstFrameUrl,
                                       String lastFrameUrl, String prompt) {
        log.info("영상 비동기 생성 시작: videoId={}", videoId);

        // 1. 상태 → PROCESSING
        updateVideoStatus(videoId, Video.GenerationStatus.PROCESSING, null);

        try {
            // 2. FAL.ai 큐에 영상 생성 작업 제출
            log.info("영상 생성 FAL.ai 제출: videoId={}, firstFrameUrl={}, lastFrameUrl={}",
                    videoId, firstFrameUrl, lastFrameUrl);
            String requestId = submitVideoJob(firstFrameUrl, lastFrameUrl, prompt);
            log.info("영상 생성 큐 제출 완료: videoId={}, requestId={}", videoId, requestId);

            // 3. 폴링으로 완료 대기
            String generatedVideoUrl = pollUntilCompleted(requestId);

            // 4. 성공 → 상태 COMPLETED, videoUrl 저장
            updateVideoStatus(videoId, Video.GenerationStatus.COMPLETED, generatedVideoUrl);
            log.info("영상 생성 완료: videoId={}, url={}", videoId, generatedVideoUrl);

        } catch (Exception e) {
            // 5. 실패 → 상태 FAILED
            log.error("영상 생성 실패: videoId={}, error={}", videoId, e.getMessage());
            updateVideoStatus(videoId, Video.GenerationStatus.FAILED, null);
        }
    }

    // ─────────────────────────────────────────
    // FAL.ai API 호출 메서드
    // ─────────────────────────────────────────

    /**
     * FAL.ai FLUX Kontext 이미지 생성 API를 동기 호출한다.
     *
     * <p>요청 바디:</p>
     * <pre>
     * {
     *   "image_url": "{referenceImageUrl}",  ← null이면 생략
     *   "prompt": "{prompt}"
     * }
     * </pre>
     *
     * <p>응답에서 {@code images[0].url}을 추출하여 반환한다.</p>
     *
     * @param referenceImageUrl 캐릭터 레퍼런스 이미지 URL (null 허용)
     * @param prompt            이미지 생성 프롬프트
     * @return 생성된 이미지 URL
     * @throws RuntimeException FAL.ai API 호출 실패 또는 응답 파싱 실패 시
     */
    /**
     * @param referenceImageUrl 캐릭터 레퍼런스 이미지 URL (null 허용)
     * @param prompt            이미지 생성 프롬프트
     * @param guidanceScale     프롬프트 충실도 (null이면 FAL.ai 기본값 사용, 높을수록 프롬프트를 더 따름)
     */
    private String callImageApi(String referenceImageUrl, String prompt, Double guidanceScale) {
        // 요청 바디 구성 (image_url은 있을 때만 포함)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", prompt);
        if (referenceImageUrl != null && !referenceImageUrl.isBlank()) {
            requestBody.put("image_url", referenceImageUrl);
        }
        // guidance_scale: 높을수록 프롬프트(스타일 등)를 강하게 반영, 낮을수록 참조 이미지 유지
        if (guidanceScale != null) {
            requestBody.put("guidance_scale", guidanceScale);
        }

        log.debug("FAL.ai 이미지 생성 요청: url={}, prompt={}", imageApiUrl, prompt);

        // 타임아웃 시 1회 재시도
        String responseJson = null;
        Exception lastException = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                responseJson = falWebClient.post()
                        .uri(imageApiUrl)
                        .bodyValue(requestBody)
                        .retrieve()
                        .onStatus(status -> status.isError(),
                                response -> response.bodyToMono(String.class)
                                        .flatMap(body -> Mono.error(
                                                new RuntimeException("FAL.ai 이미지 API 오류: " + body))))
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                        .block();
                break; // 성공 시 루프 탈출
            } catch (Exception e) {
                lastException = e;
                log.warn("FAL.ai 이미지 API 호출 실패 (attempt={}): {}", attempt + 1, e.getMessage());
                if (attempt < 1) {
                    log.info("FAL.ai 이미지 재시도 중...");
                }
            }
        }
        if (responseJson == null) {
            log.error("FAL.ai 이미지 API 최종 실패: {}", lastException.getMessage());
            throw new RuntimeException("FAL.ai 이미지 생성에 실패했습니다: " + lastException.getMessage(), lastException);
        }

        // 응답에서 images[0].url 추출
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String imageUrl = root.path("images").get(0).path("url").asText();
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new RuntimeException("FAL.ai 응답에서 이미지 URL을 찾을 수 없습니다.");
            }
            return imageUrl;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("FAL.ai 이미지 응답 파싱 실패: response={}", responseJson);
            throw new RuntimeException("FAL.ai 이미지 응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * FAL.ai FLUX Pro text-to-image API를 동기 호출한다.
     *
     * <p>요청 바디:</p>
     * <pre>
     * {
     *   "prompt": "{prompt}"
     * }
     * </pre>
     *
     * @param prompt 이미지 생성 프롬프트
     * @return 생성된 이미지 URL
     * @throws RuntimeException FAL.ai API 호출 실패 또는 응답 파싱 실패 시
     */
    private String callTextImageApi(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", prompt);

        log.debug("FAL.ai text-to-image 생성 요청: url={}, prompt={}", textImageApiUrl, prompt);

        // 타임아웃 시 1회 재시도 (callImageApi와 동일 패턴)
        String responseJson = null;
        Exception lastException = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                responseJson = falWebClient.post()
                        .uri(textImageApiUrl)
                        .bodyValue(requestBody)
                        .retrieve()
                        .onStatus(status -> status.isError(),
                                response -> response.bodyToMono(String.class)
                                        .flatMap(body -> Mono.error(
                                                new RuntimeException("FAL.ai 이미지 API 오류: " + body))))
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                        .block();
                break; // 성공 시 루프 탈출
            } catch (Exception e) {
                lastException = e;
                log.warn("FAL.ai text-to-image API 호출 실패 (attempt={}): {}", attempt + 1, e.getMessage());
                if (attempt < 1) {
                    log.info("FAL.ai text-to-image 재시도 중...");
                }
            }
        }
        if (responseJson == null) {
            log.error("FAL.ai text-to-image API 최종 실패: {}", lastException.getMessage());
            throw new RuntimeException("FAL.ai 이미지 생성에 실패했습니다: " + lastException.getMessage(), lastException);
        }

        // 응답에서 images[0].url 추출
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String imageUrl = root.path("images").get(0).path("url").asText();
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new RuntimeException("FAL.ai 응답에서 이미지 URL을 찾을 수 없습니다.");
            }
            return imageUrl;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("FAL.ai text-to-image 응답 파싱 실패: response={}", responseJson);
            throw new RuntimeException("FAL.ai 이미지 응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * FAL.ai Wan 2.1 FLF2V 영상 생성 큐에 작업을 제출한다.
     *
     * <p>요청 바디:</p>
     * <pre>
     * {
     *   "first_frame_image_url": "...",
     *   "last_frame_image_url":  "...",
     *   "prompt": "..."
     * }
     * </pre>
     *
     * @param firstFrameUrl 씬 첫 프레임 이미지 URL
     * @param lastFrameUrl  씬 마지막 프레임 이미지 URL
     * @param prompt        영상 생성 프롬프트
     * @return FAL.ai 큐 request_id
     * @throws RuntimeException 제출 실패 시
     */
    private String submitVideoJob(String firstFrameUrl, String lastFrameUrl, String prompt) {
        // FAL.ai Wan 2.1 FLF2V 필드명: start_image_url / end_image_url
        // resolution: 720p + 추론 30스텝 → 화질·자연스러움 균형
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("start_image_url", firstFrameUrl);
        requestBody.put("end_image_url", lastFrameUrl);
        requestBody.put("prompt", prompt);
        requestBody.put("resolution", "480p");            // 480p — 속도 우선 (720p 대비 ~2배 빠름)
        requestBody.put("num_inference_steps", 20);       // 20스텝 — 속도 우선 (최대 40)
        requestBody.put("guidance_scale", 5.0);           // 프롬프트 충실도 (기본값 5, 1~10 범위)

        log.debug("FAL.ai 영상 생성 큐 제출: firstFrame={}, lastFrame={}", firstFrameUrl, lastFrameUrl);

        String responseJson;
        try {
            responseJson = falWebClient.post()
                    .uri(videoApiUrl)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new RuntimeException("FAL.ai 영상 큐 제출 오류: " + body))))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception e) {
            log.error("FAL.ai 영상 큐 제출 실패: {}", e.getMessage());
            throw new RuntimeException("FAL.ai 영상 생성 큐 제출에 실패했습니다: " + e.getMessage(), e);
        }

        // request_id 추출
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String requestId = root.path("request_id").asText();
            if (requestId == null || requestId.isBlank()) {
                throw new RuntimeException("FAL.ai 응답에서 request_id를 찾을 수 없습니다.");
            }
            return requestId;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("FAL.ai 영상 큐 응답 파싱 실패: response={}", responseJson);
            throw new RuntimeException("FAL.ai 영상 큐 응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * FAL.ai 영상 생성 완료까지 지수 백오프로 폴링한다.
     *
     * <p>폴링 간격: 2초 → 4초 → 8초 → 15초(상한) 으로 증가하며,
     * 총 {@link #POLLING_TIMEOUT_MS}(약 10분) 이내에 완료되지 않으면 타임아웃한다.</p>
     *
     * <p>지수 백오프를 사용하면 초기에는 빠르게 상태를 확인하고,
     * 시간이 지나면 간격을 넓혀 불필요한 API 호출과 스레드 점유를 줄인다.</p>
     *
     * <p>상태값: IN_QUEUE → IN_PROGRESS → COMPLETED / FAILED</p>
     *
     * @param requestId FAL.ai 큐 request_id
     * @return 생성된 영상 URL
     * @throws RuntimeException FAILED 상태이거나 최대 대기 시간 초과 시
     */
    private String pollUntilCompleted(String requestId) throws InterruptedException {
        String statusUrl = videoApiUrl + "/requests/" + requestId + "/status";
        String resultUrl = videoApiUrl + "/requests/" + requestId;

        long startTime = System.currentTimeMillis();
        long currentInterval = POLLING_INITIAL_INTERVAL_MS;
        int attempt = 0;

        while (System.currentTimeMillis() - startTime < POLLING_TIMEOUT_MS) {
            Thread.sleep(currentInterval);
            attempt++;

            // 상태 조회
            String statusJson;
            try {
                statusJson = falWebClient.get()
                        .uri(statusUrl)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(30))
                        .block();
            } catch (Exception e) {
                log.warn("FAL.ai 상태 조회 실패 (attempt={}, interval={}ms): {}",
                        attempt, currentInterval, e.getMessage());
                // 실패 시에도 백오프 적용
                currentInterval = Math.min(currentInterval * 2, POLLING_MAX_INTERVAL_MS);
                continue;
            }

            // status 파싱
            String status;
            try {
                JsonNode root = objectMapper.readTree(statusJson);
                status = root.path("status").asText();
            } catch (Exception e) {
                log.warn("FAL.ai 상태 응답 파싱 실패 (attempt={}): {}", attempt, e.getMessage());
                currentInterval = Math.min(currentInterval * 2, POLLING_MAX_INTERVAL_MS);
                continue;
            }

            log.info("FAL.ai 영상 생성 상태: requestId={}, status={}, attempt={}, interval={}ms",
                    requestId, status, attempt, currentInterval);

            if ("COMPLETED".equals(status)) {
                return fetchVideoResult(resultUrl);
            } else if ("FAILED".equals(status)) {
                throw new RuntimeException("FAL.ai 영상 생성이 실패 상태로 종료되었습니다. requestId=" + requestId);
            }

            // IN_QUEUE, IN_PROGRESS → 지수 백오프로 간격 증가
            currentInterval = Math.min(currentInterval * 2, POLLING_MAX_INTERVAL_MS);
        }

        throw new RuntimeException("FAL.ai 영상 생성 타임아웃 (10분 초과). requestId=" + requestId);
    }

    /**
     * FAL.ai 영상 생성 결과를 조회하여 videoUrl을 반환한다.
     *
     * <p>응답 형식: {@code { "video": { "url": "..." } }}</p>
     *
     * @param resultUrl 결과 조회 URL
     * @return 생성된 영상 URL
     * @throws RuntimeException 조회 실패 또는 파싱 실패 시
     */
    private String fetchVideoResult(String resultUrl) {
        String responseJson;
        try {
            responseJson = falWebClient.get()
                    .uri(resultUrl)
                    .retrieve()
                    .onStatus(status -> status.isError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("FAL.ai 영상 결과 조회 오류 응답: status={}, body={}",
                                                response.statusCode(), body);
                                        return Mono.error(new RuntimeException(
                                                "FAL.ai 영상 결과 오류 " + response.statusCode() + ": " + body));
                                    }))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception e) {
            log.error("FAL.ai 영상 결과 조회 실패: {}", e.getMessage());
            throw new RuntimeException("FAL.ai 영상 결과 조회 실패: " + e.getMessage(), e);
        }

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String videoUrl = root.path("video").path("url").asText();
            if (videoUrl == null || videoUrl.isBlank()) {
                throw new RuntimeException("FAL.ai 결과에서 video URL을 찾을 수 없습니다.");
            }
            return videoUrl;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("FAL.ai 영상 결과 파싱 실패: response={}", responseJson);
            throw new RuntimeException("FAL.ai 영상 결과 파싱 실패: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────
    // 이미지 즉시 생성 (동기, DB 저장 없음)
    // ─────────────────────────────────────────

    /**
     * FAL.ai 이미지를 동기로 생성하고 URL을 즉시 반환한다.
     *
     * <p>캐릭터·배경 임시 생성에 사용된다. DB에 저장하지 않으며,
     * 프론트에서 결과를 확인 후 "프로젝트에 사용하기"를 누를 때 별도로 저장한다.</p>
     *
     * @param referenceImageUrl 레퍼런스 이미지 URL (null 허용)
     * @param prompt            이미지 생성 프롬프트
     * @return 생성된 이미지 URL
     * @throws RuntimeException FAL.ai API 호출 실패 시
     */
    public String generateImageSync(String referenceImageUrl, String prompt) {
        return callImageApi(referenceImageUrl, prompt, null);
    }

    /**
     * FAL.ai 이미지를 동기로 생성하고 URL을 즉시 반환한다 (guidance_scale 지정 버전).
     *
     * <p>스타일 변환이 필요한 경우 guidanceScale을 높여서 프롬프트 충실도를 올린다.</p>
     *
     * @param referenceImageUrl 레퍼런스 이미지 URL (null 허용)
     * @param prompt            이미지 생성 프롬프트
     * @param guidanceScale     프롬프트 충실도 (null이면 FAL.ai 기본값 사용)
     * @return 생성된 이미지 URL
     * @throws RuntimeException FAL.ai API 호출 실패 시
     */
    public String generateImageSync(String referenceImageUrl, String prompt, Double guidanceScale) {
        return callImageApi(referenceImageUrl, prompt, guidanceScale);
    }

    /**
     * FAL.ai FLUX Pro text-to-image 동기 호출 (참조 이미지 없이 프롬프트만 사용).
     *
     * <p>씬 프레임 재생성에 사용된다. FLUX Kontext와 달리 {@code image_url} 없이 호출한다.</p>
     *
     * @param prompt 이미지 생성 프롬프트
     * @return 생성된 이미지 URL
     * @throws RuntimeException FAL.ai API 호출 실패 시
     */
    public String generateTextImageSync(String prompt) {
        return callTextImageApi(prompt);
    }

    // ─────────────────────────────────────────
    // DB 상태 갱신 헬퍼
    // ─────────────────────────────────────────

    /**
     * Image 엔티티의 상태와 URL을 갱신한다.
     * {@code imageUrl}이 null이면 URL 필드는 갱신하지 않는다.
     *
     * @param imageId   갱신할 Image ID
     * @param status    변경할 상태
     * @param imageUrl  저장할 이미지 URL (null 허용)
     */
    private void updateImageStatus(String imageId, Image.GenerationStatus status, String imageUrl) {
        imageRepository.findById(imageId).ifPresent(image -> {
            image.setStatus(status);
            if (imageUrl != null) {
                image.setImageUrl(imageUrl);

                // 첫 씬의 첫 프레임 이미지 완료 시 → 프로젝트 썸네일(backgroundImageUrl) 자동 설정
                if (status == Image.GenerationStatus.COMPLETED
                        && image.getSceneNumber() == 1
                        && image.getFrameType() == Image.FrameType.FIRST) {
                    try {
                        Project project = image.getPlot().getProject();
                        if (project.getBackgroundImageUrl() == null || project.getBackgroundImageUrl().isBlank()) {
                            project.setBackgroundImageUrl(imageUrl);
                            projectRepository.save(project);
                            log.info("프로젝트 썸네일 자동 설정: projectId={}, imageUrl={}", project.getProjectId(), imageUrl);
                        }
                    } catch (Exception e) {
                        log.warn("프로젝트 썸네일 자동 설정 실패 (무시): {}", e.getMessage());
                    }
                }
            }
            imageRepository.save(image);
        });
    }

    /**
     * Video 엔티티의 상태와 URL을 갱신한다.
     * {@code videoUrl}이 null이면 URL 필드는 갱신하지 않는다.
     *
     * @param videoId  갱신할 Video ID
     * @param status   변경할 상태
     * @param videoUrl 저장할 영상 URL (null 허용)
     */
    private void updateVideoStatus(String videoId, Video.GenerationStatus status, String videoUrl) {
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(status);
            if (videoUrl != null) {
                video.setVideoUrl(videoUrl);
            }
            videoRepository.save(video);
        });
    }
}
