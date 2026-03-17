package com.estaid.content.controller;

import com.estaid.common.response.ApiResponse;
import com.estaid.content.dto.PlotBackgroundResponse;
import com.estaid.content.dto.PlotCharacterResponse;
import com.estaid.content.service.ContentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 플롯 연관 조회 API를 제공한다. */
@RestController
@RequestMapping("/plots")
@RequiredArgsConstructor
public class PlotQueryController {

    private final ContentQueryService contentQueryService;

    /** 플롯에 연결된 캐릭터 정보를 조회한다. */
    @GetMapping("/{id}/character")
    public ApiResponse<PlotCharacterResponse> getPlotCharacter(@PathVariable("id") String plotId) {
        return ApiResponse.ok(contentQueryService.getPlotCharacter(plotId));
    }

    /** 플롯에 연결된 배경 정보를 조회한다. */
    @GetMapping("/{id}/background")
    public ApiResponse<PlotBackgroundResponse> getPlotBackground(@PathVariable("id") String plotId) {
        return ApiResponse.ok(contentQueryService.getPlotBackground(plotId));
    }
}
