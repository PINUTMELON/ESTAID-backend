package com.estaid.content.dto;

import java.time.OffsetDateTime;

public record GalleryItemResponse(
        String projectId,
        String projectTitle,
        String ownerUsername,
        String plotId,
        String plotTitle,
        Integer sceneNumber,
        String thumbnailImageUrl,
        String videoId,
        String videoUrl,
        OffsetDateTime createdAt
) {
}
