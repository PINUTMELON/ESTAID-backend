package com.estaid.content.dto;

/** 플롯 캐릭터 응답. */
public record PlotCharacterResponse(
        String plotId,
        String characterId,
        String name,
        String referenceImageUrl,
        String artStyle
) {
}
