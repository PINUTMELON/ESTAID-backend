package com.estaid.domain.plot.controller;

import com.estaid.common.response.ApiResponse;
import com.estaid.domain.plot.dto.PlotCreateRequest;
import com.estaid.domain.plot.dto.PlotResponse;
import com.estaid.domain.plot.service.PlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 플롯 컨트롤러
 *
 * POST /api/plots          - 플롯 생성 (AI 플롯 기획)
 * GET  /api/plots/{plotId} - 플롯 조회
 */
@RestController
@RequestMapping("/api/plots")
@RequiredArgsConstructor
public class PlotController {

    private final PlotService plotService;

    /** 플롯 생성 */
    @PostMapping
    public ResponseEntity<ApiResponse<PlotResponse>> createPlot(
            @Valid @RequestBody PlotCreateRequest request) {

        PlotResponse response = plotService.createPlot(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("플롯이 생성되었습니다.", response));
    }

    /** 플롯 조회 */
    @GetMapping("/{plotId}")
    public ResponseEntity<ApiResponse<PlotResponse>> getPlot(
            @PathVariable String plotId) {

        PlotResponse response = plotService.getPlot(plotId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
