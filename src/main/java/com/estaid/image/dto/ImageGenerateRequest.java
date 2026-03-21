package com.estaid.image.dto;

import com.estaid.image.Image;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 이미지 생성 요청 DTO
 *
 * <p>씬의 첫 프레임 또는 마지막 프레임 이미지 생성 요청에 사용된다.
 * FAL.ai FLUX Kontext 모델이 호출되며, 플롯에 연결된 캐릭터의
 * 레퍼런스 이미지가 있으면 외형 일관성 유지를 위해 함께 전달된다.</p>
 *
 * <p>사용 예시:</p>
 * <pre>
 * POST /api/images/generate
 * {
 *   "plotId": "uuid-of-plot",
 *   "sceneNumber": 1,
 *   "frameType": "FIRST"
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
public class ImageGenerateRequest {

    /**
     * 이미지를 생성할 플롯의 UUID
     * 플롯에서 씬 목록과 캐릭터 레퍼런스 이미지를 조회한다.
     */
    @NotBlank(message = "plotId는 필수입니다.")
    private String plotId;

    /**
     * 이미지를 생성할 씬 번호 (1부터 시작)
     * 해당 씬의 firstFramePrompt 또는 lastFramePrompt가 프롬프트로 사용된다.
     */
    @NotNull(message = "sceneNumber는 필수입니다.")
    @Min(value = 1, message = "sceneNumber는 1 이상이어야 합니다.")
    private Integer sceneNumber;

    /**
     * 생성할 프레임 종류
     * FIRST: 씬 첫 프레임 → firstFramePrompt 사용
     * LAST:  씬 마지막 프레임 → lastFramePrompt 사용
     */
    @NotNull(message = "frameType은 필수입니다.")
    private Image.FrameType frameType;
}
