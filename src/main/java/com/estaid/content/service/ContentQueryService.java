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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
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
        if (projects.isEmpty()) {
            return List.of();
        }

        // ── 배치 조회: 유저, 플롯, 이미지, 비디오를 한번에 가져온다 ──
        List<String> userIds = projects.stream().map(ProjectEntity::getUserId).distinct().toList();
        Map<String, String> usernameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(com.estaid.user.User::getUserId, com.estaid.user.User::getUsername));

        List<String> projectIds = projects.stream().map(ProjectEntity::getProjectId).toList();
        List<PlotEntity> allPlots = plotRepository.findByProjectIdInOrderByCreatedAtAsc(projectIds);
        Map<String, List<PlotEntity>> plotsByProject = allPlots.stream()
                .collect(Collectors.groupingBy(PlotEntity::getProjectId, LinkedHashMap::new, Collectors.toList()));

        List<String> allPlotIds = allPlots.stream().map(PlotEntity::getPlotId).toList();
        if (allPlotIds.isEmpty()) {
            return List.of();
        }

        // 이미지: plotId+sceneNumber별로 그룹화
        Map<String, List<ImageEntity>> imagesByPlotScene = imageRepository
                .findByPlotIdInOrderByPlotIdAscSceneNumberAscFrameTypeAsc(allPlotIds).stream()
                .collect(Collectors.groupingBy(img -> img.getPlotId() + ":" + img.getSceneNumber()));

        // 비디오: plotId+sceneNumber별 최신 1건만 추출
        Map<String, VideoEntity> latestVideoByPlotScene = buildLatestVideoMap(allPlotIds);

        // ── 갤러리 아이템 구성 (추가 쿼리 없음) ──
        List<GalleryItemResponse> items = new ArrayList<>();

        for (ProjectEntity project : projects) {
            String ownerUsername = usernameMap.getOrDefault(project.getUserId(), project.getUserId());
            List<PlotEntity> plots = plotsByProject.getOrDefault(project.getProjectId(), List.of());

            for (PlotEntity plot : plots) {
                // 이미지·비디오에서 씬 번호 합산
                TreeSet<Integer> sceneNumbers = collectSceneNumbers(plot.getPlotId(), imagesByPlotScene, latestVideoByPlotScene);

                for (Integer sceneNumber : sceneNumbers) {
                    String key = plot.getPlotId() + ":" + sceneNumber;
                    VideoEntity video = latestVideoByPlotScene.get(key);
                    if (video == null) {
                        continue;
                    }

                    String thumbnailImageUrl = imagesByPlotScene.getOrDefault(key, List.of()).stream()
                            .map(ImageEntity::getImageUrl)
                            .filter(url -> url != null && !url.isBlank())
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
                            video.getVideoId(),
                            video.getVideoUrl(),
                            video.getCreatedAt()));
                }
            }
        }

        items.sort(Comparator.comparing(GalleryItemResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    public List<ProjectRankingResponse> getProjectRanking() {
        List<ProjectEntity> projects = projectRepository.findAllByOrderByAverageRatingDescRatingCountDescCreatedAtDesc();
        if (projects.isEmpty()) {
            return List.of();
        }

        // 배치 조회: 유저, 플롯, 이미지를 한번에 가져온다
        List<String> userIds = projects.stream().map(ProjectEntity::getUserId).distinct().toList();
        Map<String, String> usernameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(com.estaid.user.User::getUserId, com.estaid.user.User::getUsername));

        List<String> projectIds = projects.stream().map(ProjectEntity::getProjectId).toList();
        List<PlotEntity> allPlots = plotRepository.findByProjectIdInOrderByCreatedAtAsc(projectIds);
        // 프로젝트별 첫 번째 플롯만 추출 (대표 썸네일용)
        Map<String, PlotEntity> firstPlotByProject = new LinkedHashMap<>();
        for (PlotEntity plot : allPlots) {
            firstPlotByProject.putIfAbsent(plot.getProjectId(), plot);
        }

        // 첫 플롯들의 이미지를 배치 조회
        List<String> firstPlotIds = firstPlotByProject.values().stream()
                .map(PlotEntity::getPlotId).toList();
        Map<String, List<ImageEntity>> imagesByPlotScene = firstPlotIds.isEmpty() ? Map.of()
                : imageRepository.findByPlotIdInOrderByPlotIdAscSceneNumberAscFrameTypeAsc(firstPlotIds).stream()
                        .collect(Collectors.groupingBy(img -> img.getPlotId() + ":" + img.getSceneNumber()));

        List<ProjectRankingResponse> rankings = new ArrayList<>();
        int rank = 1;
        for (ProjectEntity project : projects) {
            String ownerUsername = usernameMap.getOrDefault(project.getUserId(), project.getUserId());

            // 대표 썸네일: 첫 플롯의 첫 씬 FIRST 프레임 이미지
            String thumbnailImageUrl = null;
            PlotEntity firstPlot = firstPlotByProject.get(project.getProjectId());
            if (firstPlot != null) {
                thumbnailImageUrl = findFirstThumbnail(firstPlot, imagesByPlotScene);
            }

            rankings.add(new ProjectRankingResponse(
                    rank++,
                    project.getProjectId(),
                    project.getTitle(),
                    ownerUsername,
                    project.getBackgroundImageUrl(),
                    thumbnailImageUrl,
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
        List<PlotEntity> plots = plotRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        List<String> plotIds = plots.stream().map(PlotEntity::getPlotId).toList();

        // ── 배치 조회: 이미지·비디오를 한번에 가져온다 ──
        Map<String, List<ImageEntity>> imagesByPlotScene = plotIds.isEmpty() ? Map.of()
                : imageRepository.findByPlotIdInOrderByPlotIdAscSceneNumberAscFrameTypeAsc(plotIds).stream()
                        .collect(Collectors.groupingBy(img -> img.getPlotId() + ":" + img.getSceneNumber()));

        Map<String, VideoEntity> latestVideoByPlotScene = plotIds.isEmpty() ? Map.of()
                : buildLatestVideoMap(plotIds);

        List<ProjectSceneDetailResponse> scenes = new ArrayList<>();

        for (PlotEntity plot : plots) {
            // scenesJson 파싱 → sceneNumber별 title/firstFrameImageUrl 추출용 맵
            List<SceneDto> sceneDtos = plot.getParsedScenes(objectMapper);

            // 씬 번호: 프리페치된 이미지·비디오에서 추출
            TreeSet<Integer> sceneNumbers = collectSceneNumbers(plot.getPlotId(), imagesByPlotScene, latestVideoByPlotScene);

            for (Integer sceneNumber : sceneNumbers) {
                String key = plot.getPlotId() + ":" + sceneNumber;
                List<ImageEntity> sceneImages = imagesByPlotScene.getOrDefault(key, List.of());

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
                    // SceneDto에 없으면 프리페치된 이미지에서 FIRST 프레임 탐색
                    thumbnail = sceneImages.stream()
                            .filter(img -> "FIRST".equals(img.getFrameType()))
                            .map(ImageEntity::getImageUrl)
                            .filter(url -> url != null && !url.isBlank())
                            .findFirst()
                            .orElse(null);
                }

                // 이미지 아이템 목록 (프리페치된 데이터 사용)
                List<SceneImageItemResponse> imageItems = sceneImages.stream()
                        .map(image -> new SceneImageItemResponse(
                                image.getImageId(),
                                image.getSceneNumber(),
                                image.getFrameType(),
                                image.getPrompt(),
                                image.getImageUrl(),
                                image.getStatus()))
                        .toList();

                // 비디오 (프리페치된 최신 비디오 사용)
                VideoEntity video = latestVideoByPlotScene.get(key);
                SceneVideoResponse videoResponse = video != null ? toSceneVideoResponse(video) : null;

                scenes.add(new ProjectSceneDetailResponse(
                        plot.getPlotId(),
                        plot.getTitle(),
                        sceneNumber,
                        sceneTitle,
                        thumbnail,
                        imageItems,
                        videoResponse));
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

        List<PlotEntity> plots = plotRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        List<String> plotIds = plots.stream().map(PlotEntity::getPlotId).toList();

        // ── 배치 조회: 이미지·비디오를 한번에 가져온다 ──
        Map<String, List<ImageEntity>> imagesByPlotScene = plotIds.isEmpty() ? Map.of()
                : imageRepository.findByPlotIdInOrderByPlotIdAscSceneNumberAscFrameTypeAsc(plotIds).stream()
                        .collect(Collectors.groupingBy(img -> img.getPlotId() + ":" + img.getSceneNumber()));

        Map<String, VideoEntity> latestVideoByPlotScene = plotIds.isEmpty() ? Map.of()
                : buildLatestVideoMap(plotIds);

        List<VideoSceneInfoResponse> sceneInfos = new ArrayList<>();

        for (PlotEntity plot : plots) {
            List<SceneDto> sceneDtos = plot.getParsedScenes(objectMapper);

            // scenesJson에 씬이 있으면 SceneDto 기반으로 구성
            if (!sceneDtos.isEmpty()) {
                for (SceneDto dto : sceneDtos) {
                    String key = plot.getPlotId() + ":" + dto.getSceneNumber();
                    List<ImageEntity> sceneImages = imagesByPlotScene.getOrDefault(key, List.of());

                    // ── firstFrameUrl ────────────────────────────────
                    String firstFrameUrl = dto.getFirstFrameImageUrl();
                    if (firstFrameUrl == null) {
                        // 프리페치된 이미지에서 FIRST 프레임으로 폴백
                        firstFrameUrl = sceneImages.stream()
                                .filter(img -> "FIRST".equals(img.getFrameType()))
                                .map(ImageEntity::getImageUrl)
                                .filter(url -> url != null && !url.isBlank())
                                .findFirst()
                                .orElse(null);
                    }

                    // ── lastFrameUrl ─────────────────────────────────
                    String lastFrameUrl = dto.getLastFrameImageUrl();
                    if (lastFrameUrl == null) {
                        // 프리페치된 이미지에서 LAST 프레임으로 폴백
                        lastFrameUrl = sceneImages.stream()
                                .filter(img -> "LAST".equals(img.getFrameType()))
                                .map(ImageEntity::getImageUrl)
                                .filter(url -> url != null && !url.isBlank())
                                .findFirst()
                                .orElse(null);
                    }

                    // ── combinedPrompt ────────────────────────────────
                    // 기존 Video.videoPrompt가 있으면 우선 사용, 없으면 프레임 프롬프트 합산
                    VideoEntity video = latestVideoByPlotScene.get(key);
                    String combinedPrompt = (video != null && video.getVideoPrompt() != null
                            && !video.getVideoPrompt().isBlank())
                            ? video.getVideoPrompt()
                            : buildCombinedPrompt(dto.getFirstFramePrompt(), dto.getLastFramePrompt());

                    sceneInfos.add(new VideoSceneInfoResponse(
                            dto.getSceneNumber(),
                            dto.getTitle(),
                            firstFrameUrl,
                            lastFrameUrl,
                            combinedPrompt));
                }
            } else {
                // scenesJson이 없으면 프리페치된 이미지 기반으로 구성 (하위 호환)
                TreeSet<Integer> sceneNumbers = collectSceneNumbers(
                        plot.getPlotId(), imagesByPlotScene, latestVideoByPlotScene);

                for (Integer sceneNumber : sceneNumbers) {
                    String key = plot.getPlotId() + ":" + sceneNumber;
                    List<ImageEntity> sceneImages = imagesByPlotScene.getOrDefault(key, List.of());

                    String firstFrameUrl = sceneImages.stream()
                            .filter(img -> "FIRST".equals(img.getFrameType()))
                            .map(ImageEntity::getImageUrl)
                            .filter(url -> url != null && !url.isBlank())
                            .findFirst().orElse(null);

                    String lastFrameUrl = sceneImages.stream()
                            .filter(img -> "LAST".equals(img.getFrameType()))
                            .map(ImageEntity::getImageUrl)
                            .filter(url -> url != null && !url.isBlank())
                            .findFirst().orElse(null);

                    VideoEntity video = latestVideoByPlotScene.get(key);
                    String combinedPrompt = video != null ? video.getVideoPrompt() : null;

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
        List<String> plotIds = plots.stream().map(PlotEntity::getPlotId).toList();

        // 배치 조회: 이미지·비디오에서 씬 번호를 한번에 추출
        Map<String, List<ImageEntity>> imagesByPlotScene = plotIds.isEmpty() ? Map.of()
                : imageRepository.findByPlotIdInOrderByPlotIdAscSceneNumberAscFrameTypeAsc(plotIds).stream()
                        .collect(Collectors.groupingBy(img -> img.getPlotId() + ":" + img.getSceneNumber()));

        Map<String, VideoEntity> latestVideoByPlotScene = plotIds.isEmpty() ? Map.of()
                : buildLatestVideoMap(plotIds);

        List<PlotSceneSummaryResponse> scenes = new ArrayList<>();
        for (PlotEntity plot : plots) {
            TreeSet<Integer> sceneNumbers = collectSceneNumbers(
                    plot.getPlotId(), imagesByPlotScene, latestVideoByPlotScene);
            scenes.add(new PlotSceneSummaryResponse(
                    plot.getPlotId(),
                    plot.getTitle(),
                    new ArrayList<>(sceneNumbers)));
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

    /**
     * 플롯의 대표 썸네일 이미지 URL을 찾는다.
     *
     * <p>탐색 우선순위:</p>
     * <ol>
     *   <li>scenesJson의 firstFrameImageUrl (가장 빠름, DB 조회 없음)</li>
     *   <li>프리페치된 이미지 맵에서 첫 씬의 FIRST 프레임</li>
     *   <li>프리페치된 이미지 중 URL이 있는 아무 이미지</li>
     * </ol>
     *
     * @param plot              대상 플롯 엔티티
     * @param imagesByPlotScene 프리페치된 이미지 맵 (plotId:sceneNumber → ImageEntity 목록)
     * @return 썸네일 이미지 URL (없으면 null)
     */
    private String findFirstThumbnail(PlotEntity plot, Map<String, List<ImageEntity>> imagesByPlotScene) {
        // 1순위: scenesJson의 firstFrameImageUrl
        List<SceneDto> scenes = plot.getParsedScenes(objectMapper);
        if (!scenes.isEmpty()) {
            String url = scenes.get(0).getFirstFrameImageUrl();
            if (url != null && !url.isBlank()) {
                return url;
            }
        }

        // 2순위: 프리페치된 이미지에서 첫 씬 FIRST 프레임
        String prefix = plot.getPlotId() + ":";
        for (Map.Entry<String, List<ImageEntity>> entry : imagesByPlotScene.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                for (ImageEntity img : entry.getValue()) {
                    if ("FIRST".equals(img.getFrameType())
                            && img.getImageUrl() != null && !img.getImageUrl().isBlank()) {
                        return img.getImageUrl();
                    }
                }
                // FIRST가 없으면 아무 이미지라도 반환
                for (ImageEntity img : entry.getValue()) {
                    if (img.getImageUrl() != null && !img.getImageUrl().isBlank()) {
                        return img.getImageUrl();
                    }
                }
            }
        }
        return null;
    }

    // ── 배치 조회 헬퍼 메서드 (N+1 방지) ──────────────────────────

    /**
     * 여러 플롯의 씬 영상을 한번에 조회하여, (plotId:sceneNumber) → 최신 VideoEntity 맵을 구성한다.
     *
     * <p>동일 (plotId, sceneNumber)에 여러 비디오가 있을 경우 createdAt DESC 순으로 정렬되어
     * 첫 번째 항목(최신)만 맵에 저장한다.</p>
     *
     * @param plotIds 조회 대상 플롯 ID 목록
     * @return (plotId:sceneNumber) 키 → 최신 VideoEntity 맵
     */
    private Map<String, VideoEntity> buildLatestVideoMap(List<String> plotIds) {
        Map<String, VideoEntity> result = new LinkedHashMap<>();
        for (VideoEntity v : videoRepository.findByPlotIdsAndVideoType(plotIds, VIDEO_TYPE_SCENE)) {
            // createdAt DESC 순이므로 첫 항목만 저장 (putIfAbsent)
            String key = v.getPlotId() + ":" + v.getSceneNumber();
            result.putIfAbsent(key, v);
        }
        return result;
    }

    /**
     * 프리페치된 이미지·비디오 맵에서 특정 플롯의 씬 번호 목록을 추출한다.
     *
     * <p>이미지 테이블과 비디오 테이블 양쪽의 씬 번호를 합산하여 정렬된 TreeSet으로 반환한다.</p>
     *
     * @param plotId                  대상 플롯 ID
     * @param imagesByPlotScene       프리페치된 이미지 맵 (plotId:sceneNumber → ImageEntity 목록)
     * @param latestVideoByPlotScene  프리페치된 비디오 맵 (plotId:sceneNumber → VideoEntity)
     * @return 정렬된 씬 번호 집합
     */
    private TreeSet<Integer> collectSceneNumbers(
            String plotId,
            Map<String, List<ImageEntity>> imagesByPlotScene,
            Map<String, VideoEntity> latestVideoByPlotScene) {

        String prefix = plotId + ":";
        TreeSet<Integer> sceneNumbers = new TreeSet<>();

        // 이미지 맵에서 해당 플롯의 씬 번호 추출
        for (String key : imagesByPlotScene.keySet()) {
            if (key.startsWith(prefix)) {
                sceneNumbers.add(Integer.parseInt(key.substring(prefix.length())));
            }
        }

        // 비디오 맵에서 해당 플롯의 씬 번호 추출
        for (String key : latestVideoByPlotScene.keySet()) {
            if (key.startsWith(prefix)) {
                sceneNumbers.add(Integer.parseInt(key.substring(prefix.length())));
            }
        }

        return sceneNumbers;
    }
}
