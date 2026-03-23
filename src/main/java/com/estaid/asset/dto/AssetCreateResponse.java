package com.estaid.asset.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 자산 통합 생성 응답 DTO
 *
 * <p>캐릭터·배경 자산을 1단계로 통합 생성한 결과를 반환한다.
 * 기존 2단계(generate → save) 방식과 달리,
 * 이미지 업로드·AI 생성·엔티티 저장이 한 번에 처리된다.</p>
 *
 * <p>연관 엔드포인트:</p>
 * <ul>
 *   <li>POST /api/projects/{projectId}/characters  - 캐릭터 통합 생성</li>
 *   <li>POST /api/projects/{projectId}/backgrounds - 배경 통합 생성</li>
 * </ul>
 */
@Getter
@Builder
public class AssetCreateResponse {

    /**
     * 생성된 자산의 고유 식별자
     * <ul>
     *   <li>캐릭터: CharacterEntity.characterId</li>
     *   <li>배경:    BackgroundEntity.backgroundId</li>
     * </ul>
     */
    private String id;

    /** 자산 이름 (사용자가 입력한 값) */
    private String name;

    /**
     * 참조 이미지 URL (Supabase Storage에 업로드된 원본 이미지)
     * FAL.ai FLUX Kontext 호출 시 image_url 파라미터로 전달된 값이다.
     */
    private String referenceImageUrl;

    /**
     * AI가 생성한 이미지 URL (FAL.ai FLUX Kontext 결과)
     * 프론트엔드에서 생성된 자산 이미지를 표시할 때 사용한다.
     */
    private String imageUrl;

    /**
     * 생성 상태 — 현재는 항상 "COMPLETED"
     * 동기 처리이므로 응답 시점에 이미 생성 완료 상태이다.
     */
    private String status;
}
