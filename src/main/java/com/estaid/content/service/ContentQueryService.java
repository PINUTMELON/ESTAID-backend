package com.estaid.content.service;

import com.estaid.common.exception.BusinessException;
import com.estaid.content.dto.ImagePromptResponse;
import com.estaid.content.dto.PlotBackgroundResponse;
import com.estaid.content.dto.PlotCharacterResponse;
import com.estaid.content.dto.PlotSceneSummaryResponse;
import com.estaid.content.dto.ProjectInfoResponse;
import com.estaid.content.dto.ProjectScenesResponse;
import com.estaid.content.dto.SceneImageItemResponse;
import com.estaid.content.dto.SceneImagesResponse;
import com.estaid.content.dto.SceneVideoResponse;
import com.estaid.content.dto.VideoPromptResponse;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 콘텐츠 조회 API의 조합 로직을 담당한다. */
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

    /** 프로젝트 기본 정보를 조회한다. */
    public ProjectInfoResponse getProjectInfo(String projectId) {
        ProjectEntity project = findProjectOrThrow(projectId);
        return new ProjectInfoResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    /** 프로젝트별 플롯/씬 요약 정보를 조회한다. */
    public ProjectScenesResponse getProjectScenes(String projectId) {
        findProjectOrThrow(projectId);

        List<PlotEntity> plots = plotRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        List<PlotSceneSummaryResponse> scenes = new ArrayList<>();

        for (PlotEntity plot : plots) {
            // 이미지/영상 기준 씬 번호를 합치고 정렬한다.
            TreeSet<Integer> sceneNumbers = new TreeSet<>();
            sceneNumbers.addAll(imageRepository.findDistinctSceneNumbersByPlotId(plot.getPlotId()));
            sceneNumbers.addAll(videoRepository.findDistinctSceneNumbersByPlotIdAndVideoType(
                    plot.getPlotId(), VIDEO_TYPE_SCENE));

            scenes.add(new PlotSceneSummaryResponse(
                    plot.getPlotId(),
                    plot.getTitle(),
                    new ArrayList<>(sceneNumbers)));
        }

        return new ProjectScenesResponse(projectId, scenes);
    }

    /** 씬 이미지 목록을 조회한다. */
    public SceneImagesResponse getSceneImages(String plotId, Integer sceneNumber) {
        List<ImageEntity> images = imageRepository.findByPlotIdAndSceneNumberOrderByFrameTypeAsc(plotId, sceneNumber);
        if (images.isEmpty()) {
            throw new BusinessException("씬 이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        List<SceneImageItemResponse> imageItems = images.stream()
                .map(image -> new SceneImageItemResponse(
                        image.getImageId(),
                        image.getSceneNumber(),
                        image.getFrameType(),
                        image.getPrompt(),
                        image.getImageUrl(),
                        image.getStatus()))
                .toList();

        return new SceneImagesResponse(plotId, sceneNumber, imageItems);
    }

    /** 씬 대표 영상을 조회한다. */
    public SceneVideoResponse getSceneVideo(String plotId, Integer sceneNumber) {
        VideoEntity video = findSceneVideoOrThrow(plotId, sceneNumber);
        return new SceneVideoResponse(
                video.getVideoId(),
                video.getPlotId(),
                video.getSceneNumber(),
                video.getVideoUrl(),
                video.getStatus(),
                video.getCreatedAt());
    }

    /** 씬 이미지 생성 프롬프트 목록을 조회한다. */
    public ImagePromptResponse getImagePrompt(String plotId, Integer sceneNumber) {
        List<ImageEntity> images = imageRepository.findByPlotIdAndSceneNumberOrderByFrameTypeAsc(plotId, sceneNumber);
        if (images.isEmpty()) {
            throw new BusinessException("씬 이미지 프롬프트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        List<String> prompts = images.stream()
                .map(ImageEntity::getPrompt)
                .filter(prompt -> prompt != null && !prompt.isBlank())
                .distinct()
                .toList();

        return new ImagePromptResponse(plotId, sceneNumber, prompts);
    }

    /** 씬 영상 생성 프롬프트를 조회한다. */
    public VideoPromptResponse getVideoPrompt(String plotId, Integer sceneNumber) {
        VideoEntity video = findSceneVideoOrThrow(plotId, sceneNumber);
        return new VideoPromptResponse(plotId, sceneNumber, video.getVideoPrompt());
    }

    /** 플롯에 연결된 캐릭터 정보를 조회한다. */
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

    /** 플롯에 연결된 배경 정보를 조회한다. */
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

    /** videoId에 해당하는 재생 URL을 반환한다. */
    public URI getVideoPlaybackUri(String videoId) {
        VideoEntity video = videoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException("영상을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (video.getVideoUrl() == null || video.getVideoUrl().isBlank()) {
            throw new BusinessException("재생 가능한 영상 URL이 없습니다.", HttpStatus.NOT_FOUND);
        }

        try {
            return URI.create(video.getVideoUrl());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("유효한 영상 URL이 아닙니다.", HttpStatus.NOT_FOUND);
        }
    }

    /** 프로젝트가 없으면 404 예외를 던진다. */
    private ProjectEntity findProjectOrThrow(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("프로젝트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    /** 플롯이 없으면 404 예외를 던진다. */
    private PlotEntity findPlotOrThrow(String plotId) {
        return plotRepository.findById(plotId)
                .orElseThrow(() -> new BusinessException("플롯을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    /** 씬 최신 영상을 조회하고 없으면 404 예외를 던진다. */
    private VideoEntity findSceneVideoOrThrow(String plotId, Integer sceneNumber) {
        return videoRepository.findTopByPlotIdAndSceneNumberAndVideoTypeOrderByCreatedAtDesc(
                        plotId, sceneNumber, VIDEO_TYPE_SCENE)
                .orElseThrow(() -> new BusinessException("씬 영상을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }
}
