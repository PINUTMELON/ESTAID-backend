package com.estaid.video;

import com.estaid.common.exception.BusinessException;
import com.estaid.common.service.ClaudeService;
import com.estaid.common.service.FalAiService;
import com.estaid.image.Image;
import com.estaid.image.ImageRepository;
import com.estaid.plot.Plot;
import com.estaid.plot.PlotRepository;
import com.estaid.plot.dto.SceneDto;
import com.estaid.video.dto.VideoGenerateRequest;
import com.estaid.video.dto.VideoPromptUpdateRequest;
import com.estaid.video.dto.VideoResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 영상 비즈니스 로직 서비스
 *
 * <p>씬별 영상을 FAL.ai Wan 2.1 FLF2V 모델로 비동기 생성한다.
 * 영상 프롬프트는 Claude API가 씬 정보를 기반으로 자동 생성한다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>{@link #generate}      - 영상 생성 요청 (즉시 PENDING 반환, 비동기 처리)</li>
 *   <li>{@link #updatePrompt}  - 영상 프롬프트 수정 + 재생성 트리거</li>
 *   <li>{@link #findById}      - 영상 단건 조회 (상태 + URL 확인)</li>
 *   <li>{@link #findAllByPlot} - 플롯의 전체 영상 목록 조회</li>
 * </ul>
 *
 * <p>생성 흐름:</p>
 * <pre>
 *   1. Plot 조회 → 씬 목록 역직렬화 → 해당 sceneNumber 씬 추출
 *   2. firstImage / lastImage 조회 (모두 COMPLETED 검증)
 *   3. Claude API → 영상 프롬프트 자동 생성
 *   4. Video 엔티티 저장 (status=PENDING)
 *   5. @Async FAL.ai 큐 제출 → 폴링 → 완료 시 videoUrl 업데이트
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final PlotRepository plotRepository;
    private final ImageRepository imageRepository;
    private final ClaudeService claudeService;
    private final FalAiService falAiService;
    private final ObjectMapper objectMapper;

    /**
     * 씬 영상 생성 요청
     *
     * <p>영상 엔티티를 즉시 PENDING 상태로 저장하고,
     * FAL.ai API 호출은 비동기(@Async)로 처리한다.</p>
     *
     * <p>두 가지 방식을 지원한다:</p>
     * <ul>
     *   <li>방식 A (신규): projectId + sceneNumber + prompt → SceneDto URL 사용, 프롬프트 직접 지정</li>
     *   <li>방식 B (기존): plotId + sceneNumber + firstImageId + lastImageId → Image 엔티티 참조</li>
     * </ul>
     *
     * @param request 영상 생성 요청 DTO
     * @return 생성된 Video 응답 DTO (status=PENDING, thumbnail 포함)
     * @throws BusinessException 플롯·씬·이미지 미존재(404), 이미지 미완성(400),
     *                           plotId/projectId 미제공(400) 시
     */
    @Transactional
    public VideoResponse generate(VideoGenerateRequest request) {
        // 1. 플롯 조회 — projectId 또는 plotId 중 하나 필수
        Plot plot;
        if (request.getProjectId() != null && !request.getProjectId().isBlank()) {
            // [방식 A] projectId로 플롯 조회 (프로젝트당 1개 제한)
            List<Plot> projectPlots = plotRepository.findByProject_ProjectId(request.getProjectId());
            if (projectPlots.isEmpty()) {
                throw new BusinessException(
                        "해당 프로젝트의 플롯이 없습니다. projectId=" + request.getProjectId(),
                        HttpStatus.NOT_FOUND);
            }
            plot = projectPlots.get(0); // 프로젝트당 1개 플롯
        } else if (request.getPlotId() != null && !request.getPlotId().isBlank()) {
            // [방식 B] plotId로 직접 조회
            plot = getPlotOrThrow(request.getPlotId());
        } else {
            throw new BusinessException("projectId 또는 plotId 중 하나는 필수입니다.", HttpStatus.BAD_REQUEST);
        }

        // 2. 씬 목록 역직렬화 → 해당 씬 추출
        List<SceneDto> sceneList = deserializeScenes(plot.getScenesJson());
        SceneDto scene = sceneList.stream()
                .filter(s -> s.getSceneNumber() == request.getSceneNumber())
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "씬 번호를 찾을 수 없습니다. sceneNumber=" + request.getSceneNumber(),
                        HttpStatus.NOT_FOUND));

        // 3. 프레임 이미지 URL + Image 엔티티 결정
        String firstFrameUrl;
        String lastFrameUrl;
        Image firstImage = null;
        Image lastImage = null;

        if (request.getFirstImageId() != null && !request.getFirstImageId().isBlank()) {
            // [방식 B] Image 엔티티 조회 및 COMPLETED 검증
            firstImage = getCompletedImageOrThrow(request.getFirstImageId(), "첫 프레임");
            lastImage = getCompletedImageOrThrow(request.getLastImageId(), "마지막 프레임");
            firstFrameUrl = firstImage.getImageUrl();
            lastFrameUrl = lastImage.getImageUrl();
        } else {
            // [방식 A] SceneDto에서 URL 직접 추출
            firstFrameUrl = scene.getFirstFrameImageUrl();
            lastFrameUrl = scene.getLastFrameImageUrl();
            if (firstFrameUrl == null || lastFrameUrl == null) {
                throw new BusinessException(
                        "첫/마지막 프레임 이미지가 아직 생성되지 않았습니다. "
                        + "POST /api/projects/plots/frames/regenerate 로 먼저 프레임 이미지를 생성하세요.",
                        HttpStatus.BAD_REQUEST);
            }
        }

        // 4. 영상 프롬프트 결정
        String videoPrompt;
        if (request.getPrompt() != null && !request.getPrompt().isBlank()) {
            // [방식 A] 사용자가 직접 제공한 프롬프트 사용
            videoPrompt = request.getPrompt();
            log.info("사용자 지정 영상 프롬프트 사용: sceneNumber={}", request.getSceneNumber());
        } else {
            // [방식 B] Claude API → 영상 프롬프트 자동 생성
            try {
                videoPrompt = claudeService.generateVideoPrompt(scene, plot.getArtStyle());
            } catch (Exception e) {
                log.warn("Claude 영상 프롬프트 생성 실패, 기본 프롬프트 사용: {}", e.getMessage());
                videoPrompt = buildFallbackPrompt(scene, plot.getArtStyle());
            }
        }

        // 5. Video 엔티티 저장 (status=PENDING)
        Video video = Video.builder()
                .plot(plot)
                .sceneNumber(request.getSceneNumber())
                .videoType(Video.VideoType.SCENE)
                .videoPrompt(videoPrompt)
                .firstImage(firstImage)   // 방식 A에서는 null
                .lastImage(lastImage)     // 방식 A에서는 null
                .status(Video.GenerationStatus.PENDING)
                .build();
        Video saved = videoRepository.save(video);
        log.info("영상 엔티티 저장 완료: videoId={}, plotId={}, sceneNumber={}",
                saved.getVideoId(), plot.getPlotId(), request.getSceneNumber());

        // 6. 비동기 FAL.ai 큐 제출 (@Async - 즉시 반환, 별도 스레드에서 처리)
        falAiService.processVideoGeneration(
                saved.getVideoId(), firstFrameUrl, lastFrameUrl, videoPrompt);

        // 7. 썸네일 = 씬의 첫 프레임 이미지 URL (Video 엔티티에 별도 컬럼 없음)
        String thumbnail = firstFrameUrl;

        return VideoResponse.from(saved, thumbnail);
    }

    /**
     * 영상 프롬프트 수정 + 재생성
     *
     * <p>Claude가 생성한 프롬프트가 마음에 들지 않을 때 사용자가 직접 수정한 후
     * 이 엔드포인트를 호출하면 수정된 프롬프트로 영상이 재생성된다.</p>
     *
     * <p>처리 흐름:</p>
     * <pre>
     *   1. Video 조회
     *   2. videoPrompt 업데이트, status → PENDING
     *   3. 비동기 FAL.ai 재호출
     * </pre>
     *
     * @param videoId 수정할 Video UUID
     * @param request 수정 요청 DTO (새 videoPrompt)
     * @return 업데이트된 Video 응답 DTO (status=PENDING)
     * @throws BusinessException 영상 미존재(404) 또는 이미지 URL 없음(400) 시
     */
    @Transactional
    public VideoResponse updatePrompt(String videoId, VideoPromptUpdateRequest request) {
        // 1. 영상 조회
        Video video = getVideoOrThrow(videoId);

        // 2. 프롬프트 업데이트, 상태 초기화
        video.setVideoPrompt(request.getVideoPrompt());
        video.setStatus(Video.GenerationStatus.PENDING);
        video.setVideoUrl(null);

        // 재생성에 필요한 이미지 URL 추출 (LAZY 로딩 - 트랜잭션 내에서 접근)
        if (video.getFirstImage() == null || video.getLastImage() == null) {
            throw new BusinessException("재생성에 필요한 이미지 정보가 없습니다.", HttpStatus.BAD_REQUEST);
        }
        String firstFrameUrl = video.getFirstImage().getImageUrl();
        String lastFrameUrl = video.getLastImage().getImageUrl();
        if (firstFrameUrl == null || lastFrameUrl == null) {
            throw new BusinessException("이미지가 아직 생성 완료되지 않았습니다.", HttpStatus.BAD_REQUEST);
        }

        Video updated = videoRepository.save(video);
        log.info("영상 프롬프트 수정 + 재생성 요청: videoId={}", videoId);

        // 3. 비동기 FAL.ai 재호출
        falAiService.processVideoGeneration(
                updated.getVideoId(), firstFrameUrl, lastFrameUrl, request.getVideoPrompt());

        return VideoResponse.from(updated);
    }

    /**
     * 영상 단건 조회
     *
     * <p>프론트엔드에서 이 엔드포인트를 폴링하여 생성 완료 여부를 확인한다.
     * {@code status=COMPLETED}이면 {@code videoUrl}에 생성된 영상 URL이 담겨 있다.</p>
     *
     * @param videoId 조회할 Video UUID
     * @return VideoResponse DTO
     * @throws BusinessException 영상 미존재(404) 시
     */
    @Transactional(readOnly = true)
    public VideoResponse findById(String videoId) {
        Video video = getVideoOrThrow(videoId);
        return VideoResponse.from(video);
    }

    /**
     * 플롯의 영상 목록 조회
     *
     * @param plotId 조회할 플롯 UUID
     * @return 해당 플롯의 영상 목록 (씬 순번 오름차순)
     * @throws BusinessException 플롯 미존재(404) 시
     */
    @Transactional(readOnly = true)
    public List<VideoResponse> findAllByPlot(String plotId) {
        getPlotOrThrow(plotId); // 플롯 존재 검증
        return videoRepository.findByPlot_PlotIdOrderBySceneNumberAsc(plotId)
                .stream()
                .map(VideoResponse::from)
                .toList();
    }

    // ─────────────────────────────────────────
    // private 헬퍼
    // ─────────────────────────────────────────

    /**
     * Claude 프롬프트 생성 실패 시 사용하는 기본(폴백) 영상 프롬프트를 구성한다.
     * 씬의 composition, background, mainStory를 조합하여 간단한 영어 프롬프트를 반환한다.
     *
     * @param scene    씬 DTO
     * @param artStyle 화풍 설정 (null 허용)
     * @return 기본 영상 프롬프트
     */
    /**
     * Claude 프롬프트 생성 실패 시 사용하는 기본(폴백) 영상 프롬프트를 구성한다.
     * 씬의 majorStory, composition, lighting, artStyle을 조합하여 영어 프롬프트를 반환한다.
     *
     * @param scene    씬 DTO
     * @param artStyle 화풍 설정 (null 허용)
     * @return 기본 영상 프롬프트
     */
    private String buildFallbackPrompt(SceneDto scene, String artStyle) {
        StringBuilder sb = new StringBuilder();
        // majorStory가 있으면 사용, 없으면 빈 문자열로 시작
        String story = scene.getMajorStory() != null ? scene.getMajorStory() : "";
        sb.append(story);
        if (scene.getComposition() != null && !scene.getComposition().isBlank()) {
            sb.append(", ").append(scene.getComposition());
        }
        if (scene.getLighting() != null && !scene.getLighting().isBlank()) {
            sb.append(", ").append(scene.getLighting());
        }
        if (artStyle != null && !artStyle.isBlank()) {
            sb.append(", style: ").append(artStyle);
        }
        sb.append(", cinematic, high quality");
        return sb.toString();
    }

    /**
     * JSON 문자열을 씬 목록으로 역직렬화한다.
     * scenesJson이 null이거나 비어 있으면 빈 리스트를 반환한다.
     *
     * @param scenesJson JSON 배열 문자열
     * @return 씬 목록 (빈 리스트 가능)
     */
    private List<SceneDto> deserializeScenes(String scenesJson) {
        if (scenesJson == null || scenesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(scenesJson, new TypeReference<List<SceneDto>>() {});
        } catch (Exception e) {
            log.error("씬 역직렬화 실패: {}", e.getMessage());
            throw new BusinessException("씬 데이터 파싱에 실패했습니다.");
        }
    }

    /**
     * 이미지를 조회하고 COMPLETED 상태인지 검증한다.
     *
     * @param imageId   조회할 Image UUID
     * @param frameDesc 프레임 설명 (에러 메시지용, 예: "첫 프레임")
     * @return COMPLETED 상태의 Image 엔티티
     * @throws BusinessException 이미지 미존재(404) 또는 미완성(400) 시
     */
    private Image getCompletedImageOrThrow(String imageId, String frameDesc) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(
                        frameDesc + " 이미지를 찾을 수 없습니다. id=" + imageId, HttpStatus.NOT_FOUND));
        if (image.getStatus() != Image.GenerationStatus.COMPLETED) {
            throw new BusinessException(
                    frameDesc + " 이미지가 아직 생성 완료되지 않았습니다. status=" + image.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }
        return image;
    }

    /** 영상 조회 공통 메서드 - 없으면 404 예외 발생 */
    private Video getVideoOrThrow(String videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(
                        "영상을 찾을 수 없습니다. id=" + videoId, HttpStatus.NOT_FOUND));
    }

    /** 플롯 조회 공통 메서드 - 없으면 404 예외 발생 */
    private Plot getPlotOrThrow(String plotId) {
        return plotRepository.findById(plotId)
                .orElseThrow(() -> new BusinessException(
                        "플롯을 찾을 수 없습니다. id=" + plotId, HttpStatus.NOT_FOUND));
    }
}
