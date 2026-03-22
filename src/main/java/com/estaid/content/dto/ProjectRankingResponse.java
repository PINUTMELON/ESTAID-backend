package com.estaid.content.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProjectRankingResponse(
        Integer rank,
        String projectId,
        String title,
        String ownerUsername,
        String backgroundImageUrl,
        BigDecimal averageRating,
        Integer ratingCount,
        OffsetDateTime createdAt
) {
}
