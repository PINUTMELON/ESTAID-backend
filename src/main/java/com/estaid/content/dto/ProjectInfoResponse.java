package com.estaid.content.dto;

import java.time.OffsetDateTime;

/** 프로젝트 기본 정보 응답. */
public record ProjectInfoResponse(
        String projectId,
        String title,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
