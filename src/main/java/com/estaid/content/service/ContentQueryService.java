package com.estaid.content.service;

import com.estaid.common.exception.BusinessException;
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
import com.estaid.content.dto.VideoPromptResponse;
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
import com.estaid.user.UserRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentQueryService {

    private static final String VIDEO_TYPE_SCENE = "SCENE";

    private final ProjectRepository projectRepository;
    private final PlotRepository plotRepository;
    private final ImageRepository imageRepository;
    private final VideoRepository videoRepository;
    private final CharacterRepository characterRepository;
    private final BackgroundRepository backgroundRepository;
    private final UserRepository userRepository;

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

    public ProjectDetailResponse getProjectDetail(String projectId, String userId) {
        ProjectEntity project = findOwnedProjectOrThrow(projectId, userId);
        List<ProjectSceneDetailResponse> scenes = new ArrayList<>();

        for (PlotEntity plot : plotRepository.findByProjectIdOrderByCreatedAtAsc(projectId)) {
            for (Integer sceneNumber : sceneNumbersForPlot(plot.getPlotId())) {
                scenes.add(new ProjectSceneDetailResponse(
                        plot.getPlotId(),
                        plot.getTitle(),
                        sceneNumber,
                        toSceneImageItems(plot.getPlotId(), sceneNumber),
                        findSceneVideo(plot.getPlotId(), sceneNumber)
                                .map(this::toSceneVideoResponse)
                                .orElse(null)));
            }
        }

        return new ProjectDetailResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getBackgroundImageUrl(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                scenes);
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
}
