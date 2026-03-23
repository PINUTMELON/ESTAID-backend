package com.estaid.content.dto;

/**
 * 영상 페이지 초기 정보 — 씬 단위 응답 DTO
 *
 * <p>영상 생성 페이지에서 각 씬의 프레임 이미지 URL과 통합 프롬프트를 제공한다.
 * 이 정보를 바탕으로 프론트엔드는 씬별 영상 생성 요청을 구성한다.</p>
 *
 * <p>연관 응답: {@link VideoPageInitResponse#scenes()}</p>
 *
 * @param sceneNumber    씬 순서 번호 (1부터 시작)
 * @param title          씬 제목 (PlotEntity.scenesJson에서 파싱, 없으면 null)
 * @param firstFrameUrl  첫 프레임 이미지 URL
 *                       (SceneDto.firstFrameImageUrl → 없으면 Image 테이블 FIRST 프레임 URL)
 * @param lastFrameUrl   마지막 프레임 이미지 URL
 *                       (SceneDto.lastFrameImageUrl  → 없으면 Image 테이블 LAST  프레임 URL)
 * @param combinedPrompt 영상 생성에 사용할 통합 프롬프트
 *                       (기존 Video.videoPrompt 우선 → 없으면 firstFramePrompt + lastFramePrompt)
 */
public record VideoSceneInfoResponse(
        Integer sceneNumber,
        String title,
        String firstFrameUrl,
        String lastFrameUrl,
        String combinedPrompt
) {
}
