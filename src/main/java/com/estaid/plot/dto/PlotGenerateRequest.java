package com.estaid.plot.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * AI 스토리보드 자동 생성 요청 DTO
 *
 * <p>사용자가 전체 줄거리를 입력하면 Claude AI가 sceneCount개의 씬을 자동 생성한다.
 * 한 프로젝트당 1번만 생성 가능하며, 이미 플롯이 존재하면 409 Conflict를 반환한다.</p>
 *
 * <p>엔드포인트: {@code POST /api/projects/plots/generate}</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlotGenerateRequest {

    /**
     * 소속 프로젝트 UUID (필수)
     * 해당 프로젝트 아래에 플롯이 생성된다.
     */
    @NotBlank(message = "프로젝트 ID는 필수입니다.")
    private String projectId;

    /**
     * 전체 줄거리 텍스트 (필수)
     * Claude AI 씬 생성 프롬프트의 핵심 입력값이다.
     * 예: "주인공이 도심 폐허에서 거대 괴물과 맞닥뜨려 싸우는 이야기"
     */
    @NotBlank(message = "스토리 설명은 필수입니다.")
    private String storyDescription;

    /**
     * 생성할 씬 수 (선택, 기본값 4, 최소 1 ~ 최대 10)
     * Claude API 호출 시 이 수만큼의 씬 JSON을 생성하도록 지시한다.
     */
    @Min(value = 1, message = "씬 수는 1 이상이어야 합니다.")
    @Max(value = 10, message = "씬 수는 10 이하이어야 합니다.")
    private Integer sceneCount = 4;

    /**
     * 영상 비율 (선택)
     * 씬 구성에 참고용으로 전달된다.
     * 예: "16:9", "9:16", "1:1", "4:3"
     */
    private String ratio;
}
