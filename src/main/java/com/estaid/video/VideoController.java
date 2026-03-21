package com.estaid.video;

import com.estaid.common.response.ApiResponse;
import com.estaid.video.dto.VideoGenerateRequest;
import com.estaid.video.dto.VideoPromptUpdateRequest;
import com.estaid.video.dto.VideoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 영상 REST 컨트롤러
 *
 * <p>기본 경로: {@code /api/videos}</p>
 *
 * <ul>
 *   <li>POST /api/videos/generate             - 씬 영상 생성 요청 (즉시 PENDING 반환)</li>
 *   <li>PUT  /api/videos/{videoId}/prompt     - 영상 프롬프트 수정 + 재생성</li>
 *   <li>GET  /api/videos/{videoId}            - 영상 단건 조회 (상태 + URL 폴링용)</li>
 *   <li>GET  /api/videos/plot/{plotId}        - 플롯의 영상 목록 조회</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /**
     * 씬 영상 생성 요청
     *
     * <p>요청 즉시 {@code status=PENDING}인 응답을 반환하고,
     * FAL.ai Wan 2.1 FLF2V 비동기 처리가 시작된다.
     * 완료 여부는 {@code GET /api/videos/{videoId}}로 폴링하여 확인한다.</p>
     *
     * @param request 영상 생성 요청 바디 (plotId, sceneNumber, firstImageId, lastImageId)
     * @return 201 Created + 생성된 영상 정보 (status=PENDING)
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<VideoResponse>> generate(
            @Valid @RequestBody VideoGenerateRequest request) {
        VideoResponse response = videoService.generate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("영상 생성을 요청했습니다.", response));
    }

    /**
     * 영상 프롬프트 수정 + 재생성
     *
     * <p>Claude가 생성한 프롬프트가 마음에 들지 않을 때 수정하여 재생성을 트리거한다.
     * 호출 즉시 {@code status=PENDING}으로 초기화되고 비동기 재생성이 시작된다.</p>
     *
     * @param videoId 수정할 영상 UUID
     * @param request 수정 요청 바디 (videoPrompt)
     * @return 200 OK + 업데이트된 영상 정보 (status=PENDING)
     */
    @PutMapping("/{videoId}/prompt")
    public ResponseEntity<ApiResponse<VideoResponse>> updatePrompt(
            @PathVariable String videoId,
            @Valid @RequestBody VideoPromptUpdateRequest request) {
        VideoResponse response = videoService.updatePrompt(videoId, request);
        return ResponseEntity.ok(ApiResponse.ok("영상 프롬프트가 수정되어 재생성을 시작했습니다.", response));
    }

    /**
     * 영상 단건 조회 (상태 폴링용)
     *
     * <p>프론트엔드에서 주기적으로 호출하여 생성 완료 여부를 확인한다.
     * {@code status=COMPLETED}이면 {@code videoUrl}에 결과가 담겨 있다.</p>
     *
     * @param videoId 조회할 영상 UUID
     * @return 200 OK + 영상 정보 (status, videoUrl 포함)
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<ApiResponse<VideoResponse>> findById(@PathVariable String videoId) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.findById(videoId)));
    }

    /**
     * 플롯의 영상 목록 조회
     *
     * <p>특정 플롯에 속한 모든 씬의 영상을 씬 순번 오름차순으로 반환한다.</p>
     *
     * @param plotId 조회할 플롯 UUID
     * @return 200 OK + 영상 목록 (씬 순번 오름차순)
     */
    @GetMapping("/plot/{plotId}")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> findAllByPlot(@PathVariable String plotId) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.findAllByPlot(plotId)));
    }
}
