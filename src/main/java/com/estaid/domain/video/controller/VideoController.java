package com.estaid.domain.video.controller;

import com.estaid.common.response.ApiResponse;
import com.estaid.domain.video.dto.VideoGenerateRequest;
import com.estaid.domain.video.dto.VideoMergeRequest;
import com.estaid.domain.video.dto.VideoResponse;
import com.estaid.domain.video.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 영상 컨트롤러
 *
 * POST /api/videos/generate          - 씬 단위 영상 생성
 * POST /api/videos/merge             - 영상 병합
 * GET  /api/videos/{videoId}          - 영상 단건 조회
 * GET  /api/videos/plot/{plotId}      - 플롯 내 씬 영상 전체 조회
 */
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /** 씬 단위 영상 생성 */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<VideoResponse>> generateVideo(
            @Valid @RequestBody VideoGenerateRequest request) {

        VideoResponse response = videoService.generateVideo(request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("영상 생성이 시작되었습니다.", response));
    }

    /** 영상 병합 */
    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<VideoResponse>> mergeVideos(
            @Valid @RequestBody VideoMergeRequest request) {

        VideoResponse response = videoService.mergeVideos(request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("영상 병합이 시작되었습니다.", response));
    }

    /** 영상 단건 조회 */
    @GetMapping("/{videoId}")
    public ResponseEntity<ApiResponse<VideoResponse>> getVideo(
            @PathVariable String videoId) {

        VideoResponse response = videoService.getVideo(videoId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /** 플롯 내 씬 영상 전체 조회 */
    @GetMapping("/plot/{plotId}")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getSceneVideosByPlot(
            @PathVariable String plotId) {

        List<VideoResponse> responses = videoService.getSceneVideosByPlot(plotId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }
}
