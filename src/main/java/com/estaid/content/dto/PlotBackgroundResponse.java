package com.estaid.content.dto;

/** 플롯 배경 응답. */
public record PlotBackgroundResponse(
        String plotId,
        String backgroundId,
        String name,
        String referenceImageUrl,
        String artStyle
) {
}
