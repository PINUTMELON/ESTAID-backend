package com.estaid.video.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 영상 생성 요청 DTO
 *
 * <p>씬 영상을 FAL.ai Wan 2.1 FLF2V로 생성할 때 사용된다.</p>
 *
 * <p>두 가지 방식을 지원한다:</p>
 *
 * <p><b>방식 A — 신규 (projectId 기반)</b></p>
 * <pre>
 * POST /api/videos/generate
 * {
 *   "projectId": "uuid-of-project",
 *   "sceneNumber": 1,
 *   "prompt": "A slow cinematic dolly-in through a dark forest..."
 * }
 * </pre>
 * <ul>
 *   <li>projectId에서 플롯을 자동 조회</li>
 *   <li>SceneDto의 firstFrameImageUrl / lastFrameImageUrl을 프레임 이미지로 사용</li>
 *   <li>prompt를 영상 생성 프롬프트로 직접 사용 (Claude 자동 생성 없음)</li>
 * </ul>
 *
 * <p><b>방식 B — 기존 (plotId 기반, 하위 호환)</b></p>
 * <pre>
 * POST /api/videos/generate
 * {
 *   "plotId": "uuid-of-plot",
 *   "sceneNumber": 1,
 *   "firstImageId": "uuid-of-first-frame-image",
 *   "lastImageId": "uuid-of-last-frame-image"
 * }
 * </pre>
 * <ul>
 *   <li>firstImageId / lastImageId로 Image 엔티티를 직접 참조</li>
 *   <li>Claude API가 씬 정보를 기반으로 영상 프롬프트를 자동 생성</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class VideoGenerateRequest {

    /**
     * [신규 방식 A] 영상을 생성할 프로젝트 UUID
     * plotId와 중 하나를 반드시 제공해야 한다.
     * projectId가 있으면 해당 프로젝트의 플롯(1개)을 자동으로 조회한다.
     */
    private String projectId;

    /**
     * [기존 방식 B] 영상을 생성할 플롯의 UUID
     * plotId 또는 projectId 중 하나를 반드시 제공해야 한다.
     */
    private String plotId;

    /**
     * 영상을 생성할 씬 번호 (1부터 시작, 필수)
     * 씬 정보(SceneDto) 조회에 사용된다.
     */
    @NotNull(message = "sceneNumber는 필수입니다.")
    @Min(value = 1, message = "sceneNumber는 1 이상이어야 합니다.")
    private Integer sceneNumber;

    /**
     * [신규 방식 A] 영상 생성 프롬프트 (사용자가 직접 제공)
     * 제공 시 Claude 자동 생성을 건너뛰고 이 값을 직접 사용한다.
     * 제공하지 않으면 Claude API가 씬 정보로 자동 생성한다.
     */
    private String prompt;

    /**
     * [기존 방식 B] 씬 첫 프레임 이미지 UUID (status=COMPLETED 이어야 함)
     * FAL.ai의 first_frame_image_url로 전달된다.
     * projectId 방식 사용 시 생략 가능 (SceneDto.firstFrameImageUrl 사용).
     */
    private String firstImageId;

    /**
     * [기존 방식 B] 씬 마지막 프레임 이미지 UUID (status=COMPLETED 이어야 함)
     * FAL.ai의 last_frame_image_url로 전달된다.
     * projectId 방식 사용 시 생략 가능 (SceneDto.lastFrameImageUrl 사용).
     */
    private String lastImageId;
}
