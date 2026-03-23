package com.estaid.content.dto;

/**
 * 프로젝트 자산(Asset) 요약 응답 DTO
 *
 * <p>프로젝트 상세 조회 시 에셋 목록을 반환할 때 사용한다.
 * 에셋의 이미지 URL과 생성 프롬프트만 포함한다.</p>
 *
 * <p>연관 응답: {@link ProjectDetailResponse#assets()}</p>
 *
 * @param assetId  에셋 고유 식별자 (UUID)
 * @param type     에셋 종류 ("CHARACTER" 또는 "BACKGROUND")
 * @param imageUrl AI가 생성한 에셋 이미지 URL (FAL.ai 결과)
 * @param prompt   이미지 생성에 사용한 프롬프트 (null 가능)
 */
public record AssetItemResponse(
        String assetId,
        String type,
        String imageUrl,
        String prompt
) {
}
