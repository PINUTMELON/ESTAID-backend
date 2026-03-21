package com.estaid.asset.dto;

import com.estaid.asset.Asset;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Asset 응답 DTO
 *
 * <p>프로젝트에 저장된 캐릭터·배경 이미지 자산 정보를 반환한다.</p>
 */
@Getter
@Builder
public class AssetResponse {

    /** Asset 고유 식별자 (UUID) */
    private String assetId;

    /** 소속 프로젝트 UUID */
    private String projectId;

    /** Asset 종류 (CHARACTER / BACKGROUND) */
    private Asset.AssetType type;

    /** 이미지 URL */
    private String imageUrl;

    /** 생성에 사용한 프롬프트 */
    private String prompt;

    /** 화풍 설정 */
    private String style;

    /** 레코드 생성 시각 */
    private OffsetDateTime createdAt;

    /**
     * {@link Asset} 엔티티로부터 응답 DTO를 생성하는 팩토리 메서드
     *
     * @param asset Asset 엔티티
     * @return AssetResponse DTO
     */
    public static AssetResponse from(Asset asset) {
        return AssetResponse.builder()
                .assetId(asset.getAssetId())
                .projectId(asset.getProject() != null ? asset.getProject().getProjectId() : null)
                .type(asset.getType())
                .imageUrl(asset.getImageUrl())
                .prompt(asset.getPrompt())
                .style(asset.getStyle())
                .createdAt(asset.getCreatedAt())
                .build();
    }
}
