package com.estaid.content.dto;

import java.util.List;

/** 프로젝트별 플롯/씬 요약 응답. */
public record ProjectScenesResponse(
        String projectId,
        List<PlotSceneSummaryResponse> scenes
) {
}
