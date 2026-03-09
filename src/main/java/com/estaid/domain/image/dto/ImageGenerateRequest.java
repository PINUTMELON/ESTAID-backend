package com.estaid.domain.image.dto;

import com.estaid.domain.image.entity.Image;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 이미지 생성 요청 DTO
 */
@Getter
public class ImageGenerateRequest {

    /** 연결된 플롯 ID */
    @NotBlank(message = "플롯 ID는 필수입니다.")
    private String plotId;

    /** 씬 순번 */
    @NotNull(message = "씬 순번은 필수입니다.")
    private Integer sceneNumber;

    /** 프레임 종류 (FIRST / LAST) */
    @NotNull(message = "프레임 종류는 필수입니다.")
    private Image.FrameType frameType;

    /** 이미지 생성 프롬프트 */
    @NotBlank(message = "프롬프트는 필수입니다.")
    private String prompt;

    /**
     * 캐릭터 레퍼런스 이미지 URL (선택)
     * - 제공 시 이미지 생성 AI에게 스타일 일관성 유지를 위한 레퍼런스로 전달한다.
     */
    private String characterReferenceImageUrl;
}
