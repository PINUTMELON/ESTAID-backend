package com.estaid.asset.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 캐릭터·배경 이미지 임시 생성 요청 DTO
 *
 * <p>FAL.ai를 호출하여 이미지를 생성하되 DB에 저장하지 않는다.
 * 결과를 확인 후 마음에 들면 {@code POST /api/projects/{id}/assets}로 저장한다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class GenerateRequest {

    /**
     * 이미지 생성 프롬프트
     * 예: "anime style young woman with long brown hair standing in cherry blossom park"
     */
    @NotBlank(message = "prompt는 필수입니다.")
    private String prompt;

    /**
     * 화풍 설정 (프롬프트 끝에 자동으로 붙여준다)
     * 예: anime, realistic, webtoon
     */
    private String style;

    /**
     * 레퍼런스 이미지 URL (선택)
     * FAL.ai FLUX Kontext의 image_url로 전달되어 외형 일관성을 유지한다.
     * 캐릭터 생성 시 기존 레퍼런스 이미지가 있으면 전달한다.
     */
    private String referenceImageUrl;
}
