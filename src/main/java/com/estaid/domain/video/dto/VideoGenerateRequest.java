package com.estaid.domain.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 영상 생성 요청 DTO
 * - 첫 프레임 이미지 + 마지막 프레임 이미지 + 영상 프롬프트로 영상을 생성한다.
 */
@Getter
public class VideoGenerateRequest {

    /** 연결된 플롯 ID */
    @NotBlank(message = "플롯 ID는 필수입니다.")
    private String plotId;

    /** 씬 순번 */
    @NotNull(message = "씬 순번은 필수입니다.")
    private Integer sceneNumber;

    /** 첫 프레임 이미지 ID */
    @NotBlank(message = "첫 프레임 이미지 ID는 필수입니다.")
    private String firstImageId;

    /** 마지막 프레임 이미지 ID */
    @NotBlank(message = "마지막 프레임 이미지 ID는 필수입니다.")
    private String lastImageId;

    /** 영상 생성 프롬프트 (카메라 움직임, 씬 묘사 포함) */
    @NotBlank(message = "영상 프롬프트는 필수입니다.")
    private String videoPrompt;

    /** 영상 길이 (초, 기본 4초) */
    private int duration = 4;
}
