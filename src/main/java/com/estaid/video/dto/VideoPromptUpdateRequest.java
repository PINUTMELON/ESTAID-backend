package com.estaid.video.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 영상 프롬프트 수정 + 재생성 요청 DTO
 *
 * <p>Claude가 자동 생성한 영상 프롬프트가 마음에 들지 않을 때,
 * 사용자가 직접 수정한 프롬프트로 영상을 재생성한다.</p>
 *
 * <p>이 요청을 받으면 서버는 다음을 수행한다:</p>
 * <ol>
 *   <li>Video.videoPrompt를 새 값으로 업데이트</li>
 *   <li>Video.status를 PENDING으로 초기화</li>
 *   <li>FAL.ai 영상 생성 비동기 재시작</li>
 * </ol>
 *
 * <p>사용 예시:</p>
 * <pre>
 * PUT /api/videos/{videoId}/prompt
 * {
 *   "videoPrompt": "A cinematic scene where the hero charges forward..."
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
public class VideoPromptUpdateRequest {

    /**
     * 사용자가 수정한 영상 생성 프롬프트
     * 이 값으로 FAL.ai 재호출이 트리거된다.
     */
    @NotBlank(message = "videoPrompt는 필수입니다.")
    private String videoPrompt;
}
