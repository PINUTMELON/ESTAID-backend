package com.estaid.content.dto;

import java.util.List;

/**
 * 영상 페이지 초기 정보 응답 DTO
 *
 * <p>영상 생성 페이지 진입 시 해당 프로젝트의 모든 씬에 대한
 * 프레임 이미지 URL과 통합 영상 프롬프트를 한 번에 반환한다.</p>
 *
 * <p>연관 엔드포인트: {@code GET /projects/{id}/video-info}</p>
 *
 * @param projectId 프로젝트 UUID
 * @param scenes    씬별 영상 초기 정보 목록 (씬 번호 오름차순)
 */
public record VideoPageInitResponse(
        String projectId,
        List<VideoSceneInfoResponse> scenes
) {
}
