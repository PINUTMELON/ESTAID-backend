package com.estaid.content.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 프로젝트 상세 조회 응답 DTO
 *
 * <p>프로젝트 상세 페이지에서 씬 목록, 에셋 목록, 프로젝트 기본 정보를 반환한다.</p>
 *
 * @param projectId          프로젝트 UUID
 * @param title              프로젝트 제목
 * @param backgroundImageUrl 프로젝트 대표 배경 이미지 URL (null 가능)
 * @param createdAt          프로젝트 생성 시각
 * @param updatedAt          프로젝트 최종 수정 시각
 * @param scenes             씬 목록 (씬 번호 오름차순)
 * @param assets             에셋 목록 — 캐릭터·배경 이미지 (생성 시각 오름차순)
 */
public record ProjectDetailResponse(
        String projectId,
        String title,
        String backgroundImageUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ProjectSceneDetailResponse> scenes,
        List<AssetItemResponse> assets
) {
}
