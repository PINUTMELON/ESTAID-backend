package com.estaid.content.service;

import com.estaid.asset.Asset;
import com.estaid.asset.AssetRepository;
import com.estaid.common.exception.BusinessException;
import com.estaid.content.dto.AssetItemResponse;
import com.estaid.content.dto.GalleryItemResponse;
import com.estaid.content.dto.ImagePromptResponse;
import com.estaid.content.dto.PlotBackgroundResponse;
import com.estaid.content.dto.PlotCharacterResponse;
import com.estaid.content.dto.PlotSceneSummaryResponse;
import com.estaid.content.dto.ProjectDetailResponse;
import com.estaid.content.dto.ProjectInfoResponse;
import com.estaid.content.dto.ProjectRankingResponse;
import com.estaid.content.dto.ProjectSceneDetailResponse;
import com.estaid.content.dto.ProjectScenesResponse;
import com.estaid.content.dto.SceneImageItemResponse;
import com.estaid.content.dto.SceneImagesResponse;
import com.estaid.content.dto.SceneVideoResponse;
import com.estaid.content.dto.VideoPageInitResponse;
import com.estaid.content.dto.VideoPromptResponse;
import com.estaid.content.dto.VideoSceneInfoResponse;
import com.estaid.content.dto.VideoUrlResponse;
import com.estaid.content.entity.BackgroundEntity;
import com.estaid.content.entity.CharacterEntity;
import com.estaid.content.entity.ImageEntity;
import com.estaid.content.entity.PlotEntity;
import com.estaid.content.entity.ProjectEntity;
import com.estaid.content.entity.VideoEntity;
import com.estaid.content.repository.BackgroundRepository;
import com.estaid.content.repository.CharacterRepository;
import com.estaid.content.repository.ImageRepository;
import com.estaid.content.repository.PlotRepository;
import com.estaid.content.repository.ProjectRepository;
import com.estaid.content.repository.VideoRepository;
import com.estaid.plot.dto.SceneDto;
import com.estaid.user.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 콘텐츠 조회 서비스
 *
 * <p>프로젝트·씬·이미지·영상·갤러리 등 조회 전용 로직을 담당한다.
 * 모든 메서드는 읽기 전용 트랜잭션({@code @Transactional(readOnly = true)})으로 실행된다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>{@link #getProjectDetail}   - 프로젝트 상세 (씬 목록 + 에셋 목록)</li>
 *   <li>{@link #getVideoPageInfo}   - 영상 페이지 초기 정보 (씬별 프레임 URL + 통합 프롬프트)</li>
 *   <li>{@link #getGallery}         - 공개 갤러리 조회</li>
 *   <li>{@link #getProjectRanking}  - 프로젝트 랭킹</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentQueryService {

    /** 씬 단위 영상 타입 문자열 (videos.video_type 컬럼 값) */
    private static final String VIDEO_TYPE_SCENE = "SCENE";

    private final ProjectRepository projectRepository;
    private final PlotRepository plotRepository;
    private final ImageRepository imageRepository;
    private final VideoRepository videoRepository;
    private final CharacterRepository characterRepository;
    private final BackgroundRepository backgroundRepository;
    private final UserRepository userRepository;

    /** 에셋 Repository (캐릭터·배경 이미지 조회용) */
    private final AssetRepository assetRepository;

    /** JSON 역직렬화 (scenesJson → List&lt;SceneDto&gt;) */
    private final ObjectMapper objectMapper;

    public List<GalleryItemResponse> getGallery(String currentUserId) {
        List<ProjectEntity> projects = projectRepository.findByUserIdNotOrderByCreatedAtDesc(currentUserId);
        List<GalleryItemResponse> items = new ArrayList<>();

        for (ProjectEntity project : projects) {
            String ownerUsername = userRepository.findById(project.getUserId())
                    .map(com.estaid.user.User::getUsername)
                    .orElse(project.getUserId());

            List<PlotEntity> plots = plotRepository.findByProjectIdOrderByCreatedAtAsc(project.getProjectId());
            for (PlotEntity plot : plots) {
                for (Integer sceneNumber : sceneNumbersForPlot(plot.getPlotId())) {
                    Optional<VideoEntity> video = findSceneVideo(plot.getPlotId(), sceneNumber);
                    if (video.isEmpty()) {
                        continue;
                    }

                    String thumbnailImageUrl = imageRepository.findByPlotIdAndSceneNumberOrderByFrameTypeAsc(
                                    plot.getPlotId(), sceneNumber)
                            .stream()
                            .map(ImageEntity::getImageUrl)
                            .filter(imageUrl -> imageUrl != null && !imageUrl.isBlank())
                            .findFirst()
                            .orElse(null);

                    items.add(new GalleryItemResponse(
                            project.getProjectId(),
                            project.getTitle(),
                            ownerUsername,
                            plot.getPlotId(),
                            plot.getTitle(),
                            sceneNumber,
                            thumbnailImageUrl,
                            video.get().getVideoId(),
                            video.get().getVideoUrl(),
                            video.get().getCreatedAt()));
                }
            }
        }

        items.sort(Comparator.comparing(GalleryItemResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    public List<ProjectRankingResponse> getProjectRanking() {
        List<ProjectEntity> projects = projectRepository.findAllByOrderByAverageRatingDescRatingCountDescCreatedAtDesc();
        List<ProjectRankingResponse> rankings = new ArrayList<>();

        int rank = 1;
        for (ProjectEntity project : projects) {
            String ownerUsername = userRepository.findById(project.getUserId())
                    .map(com.estaid.user.User::getUsername)
                    .orElse(project.getUserId());

            rankings.add(new ProjectRankingResponse(
                    rank++,
                    project.getProjectId(),
                    project.getTitle(),
                    ownerUsername,
                    project.getBackgroundImageUrl(),
                    project.getAverageRating(),
                    project.getRatingCount(),
                    project.getCreatedAt()));
        }

        return rankings;
    }

    /**
     * 프로젝트 상세 조회
     *
     * <p>프로젝트에 속한 플롯의 씬 목록과 에셋 목록을 포함하여 반환한다.</p>
     *
     * <p>씬 정보 보강:</p>
     * <ul>
     *   <li>sceneTitle  — scenesJson에서 파싱하여 추가</li>
     *   <li>thumbnail   — SceneDto.firstFrameImageUrl → Image 테이블 FIRST 프레임 순으로 탐색</li>
     * </ul>
     *
     * @param projectId 프로젝트 UUID
     * @param userId    요청자 사용자 UUID (소유권 검증)
     * @return 프로젝트 상세 응답 (씬 목록 + 에셋 목록)
     * @throws BusinessException 프로젝트 미존재(404) 또는 소유권 불일치(404) 시
     */
    public ProjectDetailResponse getProjectDetail(String projectId, String userId) {
        ProjectEntity project = findOwnedProjectOrThrow(projectId, userId);
        List<ProjectSceneDetailResponse> scenes = new ArrayList<>();

        for (PlotEntity plot : plotRepository.findByProjectIdOrderByCreatedAtAsc(projectId)) {
            // scenesJson 파싱 → sceneNumber별 title/firstFrameImageUrl 추출용 맵
            List<SceneDto> sceneDtos = parseScenes(plot.getScenesJson());

            for (Integer sceneNumber : sceneNumbersForPlot(plot.getPlotId())) {
                // SceneDto에서 해당 씬의 title과 thumbnail 탐색
                SceneDto matchedDto = sceneDtos.stream()
                        .filter(dto -> dto.getSceneNumber() == sceneNumber)
                        .findFirst()
                        .orElse(null);

                // sceneTitle: SceneDto.title 사용 (없으면 null)
                String sceneTitle = matchedDto != null ? matchedDto.getTitle() : null;

                // thumbnail: SceneDto.firstFrameImageUrl → Image 테이블 FIRST 프레임
                String thumbnail = matchedDto != null ? matchedDto.getFirstFrameImageUrl() : null;
                if (thumbnail == null) {
                    // SceneDto에 없으면 Image 테이블에서 FIRST 프레임 탐색
                    thumbnail = imageRepository.findByPlotIdAndSceneNumberOrderByFrameTypeAsc(
                                    plot.getPlotId(), sceneNumber)
                            .stream()
                            .filter(img -> "FIRST".equals(img.getFrameType()))
                            .map(ImageEntity::getImageUrl)
                            .filter(url -> url != null && !url.isBlank())
                            .findFirst()
                            .orElse(null);
                }

                scenes.add(new ProjectSceneDetailResponse(
                        plot.getPlotId(),
                        plot.getTitle(),
                        sceneNumber,
                        sceneTitle,
                        thumbnail,
                        toSceneImageItems(plot.getPlotId(), sceneNumber),
                        findSceneVideo(plot.getPlotId(), sceneNumber)
                                .map(this::toSceneVideoResponse)
                                .orElse(null)));
            }
        }

        // 에셋 목록 조회 (캐릭터·배경 이미지, 생성 시각 오름차순)
        List<AssetItemResponse> assets = assetRepository
                .findByProject_ProjectIdOrderByCreatedAtAsc(projectId)
                .stream()
                .map(asset -> new AssetItemResponse(
                        asset.getAssetId(),
                        asset.getType() != null ? asset.getType().name() : null,
                        asset.getImageUrl(),
                        asset.getPrompt()))
                .toList();

        return new ProjectDetailResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getBackgroundImageUrl(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                scenes,
                assets);
    }

    /**
     * 영상 페이지 초기 정보 조회
     *
     * <p>영상 생성 페이지 진입 시 해당 프로젝트의 씬별 프레임 이미지 URL과
     * 통합 영상 프롬프트를 반환한다.</p>
     *
     * <p>정보 탐색 우선순위:</p>
     * <ul>
     *   <li>firstFrameUrl  — SceneDto.firstFrameImageUrl → Image 테이블 FIRST 프레임</li>
     *   <li>lastFrameUrl   — SceneDto.lastFrameImageUrl  → Image 테이블 LAST  프레임</li>
     *   <li>combinedPrompt — Video.videoPrompt (기존 영상이 있으면) → firstFramePrompt + lastFramePrompt</li>
     * </ul>
     *
     * @param projectId 프로젝트 UUID
     * @param userId    요청자 사용자 UUID (소유권 검증)
     * @return 영상 페이지 초기 정보 (씬 번호 오름차순)
     * @throws BusinessException 프로젝트 미존재(404) 또는 소유권 불일치(404) 시
     */
    public VideoPageInitResponse getVideoPageInfo(String projectId, String userId) {
        findOwnedProjectOrThrow(projectId, userId);

        List<VideoSceneInfoResponse> sceneInfos = new ArrayList<>();

        for (PlotEntity plot : plotRepository.findByProjectIdOrderByCreatedAtAsc(projectId)) {
            List<SceneDto> sceneDtos = parseScenes(plot.getScenesJson());

            // scenesJson에 씬이 있으면 SceneDto 기반으로 구성
            if (!sceneDtos.isEmpty()) {
                for (SceneDto dto : sceneDtos) {
                    // ── firstFrameUrl ────────────────────────────────
                    String firstFrameUrl = dto.getFirstFrameImageUrl();
                    if (firstFrameUrl == null) {
                        // Image 테이블 FIRST 프레임으로 폴백
                        firstFrameUrl = imageRepository
                                .findByPlotIdAndSceneNumberOrderByFrameTypeAsc(
                                        plot.getPlotId(), dto.getSceneNumber())
                                .stream()
                                .filter(img -> "FIRST".equals(img.getFrameType()))
                                .map(ImageEntity::getImageUrl)
                                .filter(url -> url != null && !url.isBlank())
                                .findFirst()
                                .orElse(null);
                    }

                    // ── lastFrameUrl ─────────────────────────────────
                    String lastFrameUrl = dto.getLastFrameImageUrl();
                    if (lastFrameUrl == null) {
                        // Image 테이블 LAST 프레임으로 폴백
                        lastFrameUrl = imageRepository
                                .findByPlotIdAndSceneNumberOrderByFrameTypeAsc(
                                        plot.getPlotId(), dto.getSceneNumber())
                                .stream()
                                .filter(img -> "LAST".equals(img.getFrameType()))
                                .map(ImageEntity::getImageUrl)
                                .filter(url -> url != null && !url.isBlank())
                                .findFirst()
                                .orElse(null);
                    }

                    // ── combinedPrompt ────────────────────────────────
                    // 기존 Video.videoPrompt가 있으면 우선 사용, 없으면 프레임 프롬프트 합산
                    String combinedPrompt = findSceneVideo(plot.getPlotId(), dto.getSceneNumber())
                            .map(VideoEntity::getVideoPrompt)
                            .filter(p -> p != null && !p.isBlank())
                            .orElseGet(() -> buildCombinedPrompt(
                                    dto.getFirstFramePrompt(), dto.getLastFramePrompt()));

                    sceneInfos.add(new VideoSceneInfoResponse(
                            dto.getSceneNumber(),
                            dto.getTitle(),
                            firstFrameUrl,
                            lastFrameUrl,
                            combinedPrompt));
                }
            } else {
                // scenesJson이 없으면 Image 테이블 기반으로 구성 (하위 호환)
                for (Integer sceneNumber : sceneNumbersForPlot(plot.getPlotId())) {
                    List<ImageEntity> images = imageRepository
                            .findByPlotIdAndSceneNumberOrderByFrameTypeAsc(
                                    plot.getPlotId(), sceneNumber);

                    String firstFrameUrl = images.stream()
                            .filter(img -> "FIRST".equals(img.getFrameType()))
                            .map(ImageEntity::getImageUrl)
                            .filter(url -> url != null && !url.isBlank())
                            .findFirst().orElse(null);

                    String lastFrameUrl = images.stream()
                            .filter(img -> "LAST".equals(img.getFrameType()))
                            .map(ImageEntity::getImageUrl)
                            .filter(url -> url != null && !url.isBlank())
                            .findFirst().orElse(null);

                    String combinedPrompt = findSceneVideo(plot.getPlotId(), sceneNumber)
                            .map(VideoEntity::getVideoPrompt)
                            .orElse(null);

                    sceneInfos.add(new VideoSceneInfoResponse(
                            sceneNumber, null, firstFrameUrl, lastFrameUrl, combinedPrompt));
                }
            }
        }

        // 씬 번호 오름차순 정렬
        sceneInfos.sort(Comparator.comparing(VideoSceneInfoResponse::sceneNumber,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return new VideoPageInitResponse(projectId, sceneInfos);
    }

    public ProjectInfoResponse getProjectInfo(String projectId, String userId) {
        ProjectEntity project = findOwnedProjectOrThrow(projectId, userId);
        return new ProjectInfoResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    public ProjectScenesResponse getProjectScenes(String projectId, String userId) {
        findOwnedProjectOrThrow(projectId, userId);

        List<PlotEntity> plots = plotRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        List<PlotSceneSummaryResponse> scenes = new ArrayList<>();

        for (PlotEntity plot : plots) {
            scenes.add(new PlotSceneSummaryResponse(
                    plot.getPlotId(),
                    plot.getTitle(),
                    new ArrayList<>(sceneNumbersForPlot(plot.getPlotId()))));
        }

        return new ProjectScenesResponse(projectId, scenes);
    }

    public SceneImagesResponse getSceneImages(String plotId, Integer sceneNumber) {
        List<SceneImageItemResponse> imageItems = toSceneImageItems(plotId, sceneNumber);
        if (imageItems.isEmpty()) {
            throw new BusinessException("씬 이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        return new SceneImagesResponse(plotId, sceneNumber, imageItems);
    }

    public SceneVideoResponse getSceneVideo(String plotId, Integer sceneNumber) {
        return toSceneVideoResponse(findSceneVideoOrThrow(plotId, sceneNumber));
    }

    public VideoUrlResponse getVideoUrl(String videoId) {
        VideoEntity video = findVideoOrThrow(videoId);
        if (video.getVideoUrl() == null || video.getVideoUrl().isBlank()) {
            throw new BusinessException("재생 가능한 영상 URL이 없습니다.", HttpStatus.NOT_FOUND);
        }
        return new VideoUrlResponse(video.getVideoId(), video.getVideoUrl());
    }

    public VideoUrlResponse getRandomOtherUserVideo(String currentUserId) {
        VideoEntity video = videoRepository.findRandomByVideoTypeAndProjectUserIdNot(VIDEO_TYPE_SCENE, currentUserId)
                .orElseThrow(() -> new BusinessException("평가할 다른 사용자의 영상을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        return new VideoUrlResponse(video.getVideoId(), video.getVideoUrl());
    }

    public ImagePromptResponse getImagePrompt(String plotId, Integer sceneNumber) {
        List<ImageEntity> images = imageRepository.findByPlotIdAndSceneNumberOrderByFrameTypeAsc(plotId, sceneNumber);
        if (images.isEmpty()) {
            throw new BusinessException("이미지 프롬프트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        List<String> prompts = images.stream()
                .map(ImageEntity::getPrompt)
                .filter(prompt -> prompt != null && !prompt.isBlank())
                .distinct()
                .toList();

        return new ImagePromptResponse(plotId, sceneNumber, prompts);
    }

    public VideoPromptResponse getVideoPrompt(String plotId, Integer sceneNumber) {
        VideoEntity video = findSceneVideoOrThrow(plotId, sceneNumber);
        return new VideoPromptResponse(plotId, sceneNumber, video.getVideoPrompt());
    }

    public PlotCharacterResponse getPlotCharacter(String plotId) {
        PlotEntity plot = findPlotOrThrow(plotId);
        if (plot.getCharacterId() == null || plot.getCharacterId().isBlank()) {
            throw new BusinessException("플롯에 연결된 캐릭터가 없습니다.", HttpStatus.NOT_FOUND);
        }

        CharacterEntity character = characterRepository.findById(plot.getCharacterId())
                .orElseThrow(() -> new BusinessException("캐릭터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        return new PlotCharacterResponse(
                plotId,
                character.getCharacterId(),
                character.getName(),
                character.getReferenceImageUrl(),
                character.getArtStyle());
    }

    public PlotBackgroundResponse getPlotBackground(String plotId) {
        PlotEntity plot = findPlotOrThrow(plotId);
        if (plot.getBackgroundId() == null || plot.getBackgroundId().isBlank()) {
            throw new BusinessException("플롯에 연결된 배경이 없습니다.", HttpStatus.NOT_FOUND);
        }

        BackgroundEntity background = backgroundRepository.findById(plot.getBackgroundId())
                .orElseThrow(() -> new BusinessException("배경을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        return new PlotBackgroundResponse(
                plotId,
                background.getBackgroundId(),
                background.getName(),
                background.getReferenceImageUrl(),
                background.getArtStyle());
    }

    public URI getVideoPlaybackUri(String videoId) {
        VideoEntity video = findVideoOrThrow(videoId);
        if (video.getVideoUrl() == null || video.getVideoUrl().isBlank()) {
            throw new BusinessException("재생 가능한 영상 URL이 없습니다.", HttpStatus.NOT_FOUND);
        }

        try {
            return URI.create(video.getVideoUrl());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("유효한 영상 URL이 아닙니다.", HttpStatus.NOT_FOUND);
        }
    }

    private ProjectEntity findOwnedProjectOrThrow(String projectId, String userId) {
        return projectRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException("프로젝트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private PlotEntity findPlotOrThrow(String plotId) {
        return plotRepository.findById(plotId)
                .orElseThrow(() -> new BusinessException("플롯을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private VideoEntity findVideoOrThrow(String videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException("영상을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private VideoEntity findSceneVideoOrThrow(String plotId, Integer sceneNumber) {
        return findSceneVideo(plotId, sceneNumber)
                .orElseThrow(() -> new BusinessException("씬 영상을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private Optional<VideoEntity> findSceneVideo(String plotId, Integer sceneNumber) {
        return videoRepository.findTopByPlotIdAndSceneNumberAndVideoTypeOrderByCreatedAtDesc(
                plotId, sceneNumber, VIDEO_TYPE_SCENE);
    }

    private TreeSet<Integer> sceneNumbersForPlot(String plotId) {
        TreeSet<Integer> sceneNumbers = new TreeSet<>();
        sceneNumbers.addAll(imageRepository.findDistinctSceneNumbersByPlotId(plotId));
        sceneNumbers.addAll(videoRepository.findDistinctSceneNumbersByPlotIdAndVideoType(plotId, VIDEO_TYPE_SCENE));
        return sceneNumbers;
    }

    private List<SceneImageItemResponse> toSceneImageItems(String plotId, Integer sceneNumber) {
        return imageRepository.findByPlotIdAndSceneNumberOrderByFrameTypeAsc(plotId, sceneNumber)
                .stream()
                .map(image -> new SceneImageItemResponse(
                        image.getImageId(),
                        image.getSceneNumber(),
                        image.getFrameType(),
                        image.getPrompt(),
                        image.getImageUrl(),
                        image.getStatus()))
                .toList();
    }

    private SceneVideoResponse toSceneVideoResponse(VideoEntity video) {
        return new SceneVideoResponse(
                video.getVideoId(),
                video.getPlotId(),
                video.getSceneNumber(),
                video.getVideoUrl(),
                video.getStatus(),
                video.getCreatedAt());
    }

    /**
     * PlotEntity.scenesJson(JSON 문자열)을 SceneDto 목록으로 역직렬화한다.
     *
     * <p>scenesJson이 null이거나 비어 있으면 빈 리스트를 반환한다.
     * 역직렬화 실패 시 경고 로그를 남기고 빈 리스트를 반환한다.</p>
     *
     * @param scenesJson JSON 배열 문자열 (null 허용)
     * @return 씬 DTO 목록 (빈 리스트 가능)
     */
    private List<SceneDto> parseScenes(String scenesJson) {
        if (scenesJson == null || scenesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(scenesJson, new TypeReference<List<SceneDto>>() {});
        } catch (Exception e) {
            log.warn("scenesJson 파싱 실패 (빈 리스트 반환): {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 첫 프레임 프롬프트와 마지막 프레임 프롬프트를 합산하여 통합 프롬프트를 구성한다.
     *
     * @param firstFramePrompt 첫 프레임 이미지 생성 프롬프트 (null 허용)
     * @param lastFramePrompt  마지막 프레임 이미지 생성 프롬프트 (null 허용)
     * @return 합산 프롬프트 (양쪽 모두 null이면 null)
     */
    private String buildCombinedPrompt(String firstFramePrompt, String lastFramePrompt) {
        if (firstFramePrompt == null && lastFramePrompt == null) {
            return null;
        }
        if (firstFramePrompt == null) {
            return lastFramePrompt;
        }
        if (lastFramePrompt == null) {
            return firstFramePrompt;
        }
        return firstFramePrompt + " " + lastFramePrompt;
    }
}
