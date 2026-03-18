package com.estaid.content.controller;

import com.estaid.common.response.ApiResponse;
import com.estaid.content.dto.ImagePromptResponse;
import com.estaid.content.dto.SceneImagesResponse;
import com.estaid.content.dto.SceneVideoResponse;
import com.estaid.content.dto.VideoPromptResponse;
import com.estaid.content.service.ContentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 씬 단위 조회 API를 제공한다. */
@RestController
@RequestMapping("/scenes")
@RequiredArgsConstructor
public class SceneQueryController {

    private final ContentQueryService contentQueryService;

    /** 씬 이미지 목록을 조회한다. */
    @GetMapping("/{plotId}/{sceneNumber}/images")
    public ApiResponse<SceneImagesResponse> getSceneImages(
            @PathVariable String plotId,
            @PathVariable Integer sceneNumber) {
        return ApiResponse.ok(contentQueryService.getSceneImages(plotId, sceneNumber));
    }

    /** 씬 대표 영상 정보를 조회한다. */
    @GetMapping("/{plotId}/{sceneNumber}/video")
    public ApiResponse<SceneVideoResponse> getSceneVideo(
            @PathVariable String plotId,
            @PathVariable Integer sceneNumber) {
        return ApiResponse.ok(contentQueryService.getSceneVideo(plotId, sceneNumber));
    }

    /** 씬 이미지 생성 프롬프트 목록을 조회한다. */
    @GetMapping("/{plotId}/{sceneNumber}/image-prompt")
    public ApiResponse<ImagePromptResponse> getImagePrompt(
            @PathVariable String plotId,
            @PathVariable Integer sceneNumber) {
        return ApiResponse.ok(contentQueryService.getImagePrompt(plotId, sceneNumber));
    }

    /** 씬 영상 생성 프롬프트를 조회한다. */
    @GetMapping("/{plotId}/{sceneNumber}/video-prompt")
    public ApiResponse<VideoPromptResponse> getVideoPrompt(
            @PathVariable String plotId,
            @PathVariable Integer sceneNumber) {
        return ApiResponse.ok(contentQueryService.getVideoPrompt(plotId, sceneNumber));
    }
}
