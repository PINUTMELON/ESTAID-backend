package com.estaid.domain.image.controller;

import com.estaid.common.response.ApiResponse;
import com.estaid.domain.image.dto.ImageGenerateRequest;
import com.estaid.domain.image.dto.ImageResponse;
import com.estaid.domain.image.service.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 이미지 컨트롤러
 *
 * POST /api/images/generate          - 이미지 생성 요청
 * GET  /api/images/{imageId}          - 이미지 단건 조회 (상태 확인용)
 * GET  /api/images/plot/{plotId}      - 플롯 내 전체 이미지 조회
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    /** 이미지 생성 요청 */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ImageResponse>> generateImage(
            @Valid @RequestBody ImageGenerateRequest request) {

        ImageResponse response = imageService.generateImage(request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("이미지 생성이 시작되었습니다.", response));
    }

    /** 이미지 단건 조회 */
    @GetMapping("/{imageId}")
    public ResponseEntity<ApiResponse<ImageResponse>> getImage(
            @PathVariable String imageId) {

        ImageResponse response = imageService.getImage(imageId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /** 플롯 내 전체 이미지 조회 */
    @GetMapping("/plot/{plotId}")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getImagesByPlot(
            @PathVariable String plotId) {

        List<ImageResponse> responses = imageService.getImagesByPlot(plotId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }
}
