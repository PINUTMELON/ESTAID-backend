package com.estaid.content.dto;

import java.time.OffsetDateTime;

/** 씬 대표 영상 응답. */
public record SceneVideoResponse(
        String videoId,
        String plotId,
        Integer sceneNumber,
        String videoUrl,
        String status,
        OffsetDateTime createdAt
) {
}
