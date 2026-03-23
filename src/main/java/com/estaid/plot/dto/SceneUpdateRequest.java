package com.estaid.plot.dto;

import lombok.*;

/**
 * 씬 수정 요청 DTO
 *
 * <p>Claude가 생성한 씬 내용을 사용자가 직접 편집할 때 사용한다.
 * PUT /api/plots/{plotId}/scenes/{sceneNumber} 요청 바디로 전달된다.</p>
 *
 * <p>수정 흐름:</p>
 * <pre>
 *   1. PlotService가 scenes_json을 {@code List<SceneDto>}로 역직렬화
 *   2. sceneNumber에 해당하는 씬을 이 DTO의 값으로 교체
 *   3. 재직렬화하여 scenes_json 컬럼에 저장
 * </pre>
 *
 * <p>null로 전송된 필드는 그대로 null로 반영되므로,
 * 클라이언트는 유지할 필드도 기존 값을 포함하여 전송해야 한다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SceneUpdateRequest {

    /** 등장인물 (예: "주인공 단독", "주인공, 괴물") */
    private String characters;

    /**
     * 카메라 구도 — Enum 10가지 값 중 하나.
     * EXTREME_CLOSEUP / HIGH_ANGLE / LOW_ANGLE / MEDIUM_SHOT / OVER_THE_SHOULDER /
     * TWO_SHOT / WIDE_SHOT / BIRD_EYE_VIEW / CLOSEUP / DUTCH_ANGLE
     */
    private String composition;

    /** 배경 장소 이름 (1~3단어, 예: "벚꽃 공원") */
    private String background;

    /** 배경 상세 묘사 (영어, 이미지 생성 프롬프트용) */
    private String backgroundDetail;

    /** 조명·분위기 (예: "오후 햇살, 따뜻한 오렌지 톤") */
    private String lighting;

    /** 씬에 대한 구체적 설명/사건 (2~3문장, 기존 mainStory 대체) */
    private String majorStory;

    /** 수정된 첫 프레임 영어 이미지 생성 프롬프트 */
    private String firstFramePrompt;

    /** 수정된 첫 프레임 이미지 URL (재생성 후 업데이트) */
    private String firstFrameImageUrl;

    /** 수정된 마지막 프레임 영어 이미지 생성 프롬프트 */
    private String lastFramePrompt;

    /** 수정된 마지막 프레임 이미지 URL (재생성 후 업데이트) */
    private String lastFrameImageUrl;
}
