package com.estaid.content.dto;

import java.util.List;

/** 플롯별 씬 번호 요약 응답. */
public record PlotSceneSummaryResponse(
        String plotId,
        String title,
        List<Integer> sceneNumbers
) {
}
