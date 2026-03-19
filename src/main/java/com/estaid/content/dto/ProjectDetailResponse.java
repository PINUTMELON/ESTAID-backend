package com.estaid.content.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ProjectDetailResponse(
        String projectId,
        String title,
        String backgroundImageUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ProjectSceneDetailResponse> scenes
) {
}
