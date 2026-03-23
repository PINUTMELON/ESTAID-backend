package com.estaid.plot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 프레임 이미지 재생성 요청 DTO
 *
 * <p>씬의 첫 프레임 또는 마지막 프레임 이미지를 FAL.ai로 재생성한다.
 * 사용자가 프롬프트를 수정하거나 이미지가 마음에 들지 않을 때 호출한다.</p>
 *
 * <p>엔드포인트: {@code POST /api/projects/plots/frames/regenerate}</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FrameRegenerateRequest {

    /**
     * 소속 프로젝트 UUID (필수)
     * 해당 프로젝트의 플롯에서 씬을 찾는다.
     */
    @NotBlank(message = "프로젝트 ID는 필수입니다.")
    private String projectId;

    /**
     * 재생성할 씬 번호 (필수, 1 이상)
     */
    @NotNull(message = "씬 번호는 필수입니다.")
    @Min(value = 1, message = "씬 번호는 1 이상이어야 합니다.")
    private Integer sceneNumber;

    /**
     * 재생성할 프레임 위치 (필수)
     * "FIRST" = 시작 프레임, "LAST" = 끝 프레임
     */
    @NotBlank(message = "firstOrLast는 필수입니다. (FIRST 또는 LAST)")
    private String firstOrLast;

    /**
     * 이미지 생성에 사용할 영어 프롬프트 (필수)
     * 사용자가 수정하거나 기존 프롬프트를 그대로 전달한다.
     */
    @NotBlank(message = "프롬프트는 필수입니다.")
    private String prompt;
}
