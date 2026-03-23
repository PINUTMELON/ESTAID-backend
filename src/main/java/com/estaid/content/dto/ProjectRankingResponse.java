package com.estaid.content.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 프로젝트 랭킹 응답
 *
 * @param thumbnailImageUrl 대표 썸네일 (첫 씬의 FIRST 프레임 이미지 URL, 없으면 null)
 */
public record ProjectRankingResponse(
        Integer rank,
        String projectId,
        String title,
        String ownerUsername,
        String backgroundImageUrl,
        String thumbnailImageUrl,
        BigDecimal averageRating,
        Integer ratingCount,
        OffsetDateTime createdAt
) {
}
