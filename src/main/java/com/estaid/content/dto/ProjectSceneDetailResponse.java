package com.estaid.content.dto;

import java.util.List;

/**
 * 프로젝트 상세 조회 — 씬 단위 응답 DTO
 *
 * <p>프로젝트 상세 페이지에서 각 씬의 정보(이미지·영상·제목·썸네일)를 반환한다.</p>
 *
 * @param plotId      씬이 속한 플롯 UUID
 * @param plotTitle   플롯 제목
 * @param sceneNumber 씬 순서 번호 (1부터 시작)
 * @param sceneTitle  씬 제목 (PlotEntity.scenesJson에서 파싱, 없으면 null)
 * @param thumbnail   썸네일 이미지 URL
 *                    (SceneDto.firstFrameImageUrl → 없으면 Image 테이블 FIRST 프레임 URL → 없으면 null)
 * @param images      씬의 이미지 목록 (프레임 타입 순)
 * @param video       씬의 영상 정보 (없으면 null)
 */
public record ProjectSceneDetailResponse(
        String plotId,
        String plotTitle,
        Integer sceneNumber,
        String sceneTitle,
        String thumbnail,
        List<SceneImageItemResponse> images,
        SceneVideoResponse video
) {
}
