package com.estaid.asset.dto;

import com.estaid.asset.Asset;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * "프로젝트에 사용하기" Asset 저장 요청 DTO
 *
 * <p>임시 생성된 이미지를 프로젝트에 확정 저장할 때 사용한다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class AssetSaveRequest {

    /**
     * Asset 종류
     * CHARACTER: 캐릭터 이미지
     * BACKGROUND: 배경 이미지
     */
    @NotNull(message = "type은 필수입니다.")
    private Asset.AssetType type;

    /**
     * 저장할 이미지 URL (임시 생성 API에서 받은 URL)
     */
    @NotBlank(message = "imageUrl은 필수입니다.")
    private String imageUrl;

    /**
     * 이미지 생성에 사용한 프롬프트
     */
    private String prompt;

    /**
     * 화풍 설정 (anime, realistic, webtoon 등)
     */
    private String style;
}
