package com.estaid.video.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 영상 생성 요청 DTO
 *
 * <p>씬의 첫/마지막 프레임 이미지로 영상을 생성할 때 사용된다.
 * 두 이미지는 모두 {@code status=COMPLETED} 상태여야 한다.
 * Claude API가 씬 정보를 기반으로 영상 프롬프트를 자동 생성하며,
 * FAL.ai Wan 2.1 FLF2V 모델로 3~5초 영상이 생성된다.</p>
 *
 * <p>사용 예시:</p>
 * <pre>
 * POST /api/videos/generate
 * {
 *   "plotId": "uuid-of-plot",
 *   "sceneNumber": 1,
 *   "firstImageId": "uuid-of-first-frame-image",
 *   "lastImageId": "uuid-of-last-frame-image"
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
public class VideoGenerateRequest {

    /**
     * 영상을 생성할 플롯의 UUID
     * 씬 정보 조회 및 영상 프롬프트 생성에 사용된다.
     */
    @NotBlank(message = "plotId는 필수입니다.")
    private String plotId;

    /**
     * 영상을 생성할 씬 번호 (1부터 시작)
     * 씬 정보(mainStory, composition 등)가 Claude 프롬프트 생성에 사용된다.
     */
    @NotNull(message = "sceneNumber는 필수입니다.")
    @Min(value = 1, message = "sceneNumber는 1 이상이어야 합니다.")
    private Integer sceneNumber;

    /**
     * 씬 첫 프레임 이미지 UUID (status=COMPLETED 이어야 함)
     * FAL.ai의 first_frame_image_url로 전달된다.
     */
    @NotBlank(message = "firstImageId는 필수입니다.")
    private String firstImageId;

    /**
     * 씬 마지막 프레임 이미지 UUID (status=COMPLETED 이어야 함)
     * FAL.ai의 last_frame_image_url로 전달된다.
     */
    @NotBlank(message = "lastImageId는 필수입니다.")
    private String lastImageId;
}
