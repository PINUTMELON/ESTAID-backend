package com.estaid.plot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * 씬(Scene) 데이터 DTO
 *
 * <p>Claude AI가 생성하거나 사용자가 수정하는 씬 단위 정보를 담는다.
 * {@code Plot.scenesJson} 컬럼에 JSON 배열로 직렬화되어 저장되며,
 * 조회 시 {@code ObjectMapper}로 역직렬화하여 사용한다.</p>
 *
 * <p>각 씬은 영상 제작에 필요한 모든 정보를 포함한다:</p>
 * <ul>
 *   <li>sceneNumber       - 씬 순서 (1부터 시작)</li>
 *   <li>title             - 씬 제목</li>
 *   <li>characters        - 등장인물</li>
 *   <li>composition       - 카메라 구도 Enum (10가지 값 중 하나)</li>
 *   <li>background        - 배경 장소 이름 (1.1 자산 생성에서 만든 배경 이름)</li>
 *   <li>backgroundDetail  - 배경 상세 묘사 (AI가 생성한 영어 이미지 생성용 텍스트)</li>
 *   <li>lighting          - 조명·분위기</li>
 *   <li>majorStory        - 씬에 대한 구체적 설명/사건 (2~3문장)</li>
 *   <li>firstFramePrompt  - 첫 프레임 이미지 생성 영어 프롬프트</li>
 *   <li>firstFrameImageUrl - 첫 프레임 이미지 URL (비동기 생성 전 null)</li>
 *   <li>lastFramePrompt   - 마지막 프레임 이미지 생성 영어 프롬프트</li>
 *   <li>lastFrameImageUrl  - 마지막 프레임 이미지 URL (비동기 생성 전 null)</li>
 * </ul>
 *
 * <p>composition 허용 값 (Claude가 반드시 아래 10가지 중 하나만 반환):</p>
 * <ul>
 *   <li>EXTREME_CLOSEUP   - 극도의 클로즈업</li>
 *   <li>HIGH_ANGLE        - 위에서 아래를 보는 부감</li>
 *   <li>LOW_ANGLE         - 아래에서 위를 보는 앙굴</li>
 *   <li>MEDIUM_SHOT       - 인물의 상반신 위주</li>
 *   <li>OVER_THE_SHOULDER - 어깨 너머 샷</li>
 *   <li>TWO_SHOT          - 두 명의 인물이 등장하는 샷</li>
 *   <li>WIDE_SHOT         - 먼 거리에서 배경과 함께 보여주는 샷</li>
 *   <li>BIRD_EYE_VIEW     - 아주 높은 곳에서 수직으로 내려다보는 샷</li>
 *   <li>CLOSEUP           - 인물의 얼굴에 집중</li>
 *   <li>DUTCH_ANGLE       - 화면을 비스듬히 기울인 불안정한 느낌의 샷</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SceneDto {

    /** 씬 순서 번호 (1부터 시작) */
    private int sceneNumber;

    /** 씬 제목 (예: "괴물의 등장", "최후의 대결") */
    private String title;

    /** 등장인물 (예: "주인공, 괴물") */
    private String characters;

    /**
     * 카메라 구도 — Claude가 반드시 아래 10가지 Enum 값 중 하나만 반환한다.
     * EXTREME_CLOSEUP / HIGH_ANGLE / LOW_ANGLE / MEDIUM_SHOT / OVER_THE_SHOULDER
     * TWO_SHOT / WIDE_SHOT / BIRD_EYE_VIEW / CLOSEUP / DUTCH_ANGLE
     */
    private String composition;

    /**
     * 배경 장소 이름 (1~3단어)
     * 예: "어두운 숲", "도심 옥상", "폐허가 된 성"
     * 1.1 자산 생성에서 만든 배경 자산의 이름과 연결된다.
     */
    private String background;

    /**
     * 배경 상세 묘사 (AI 생성, 영어)
     * 이미지 생성 프롬프트에 활용되는 구체적인 배경 묘사 텍스트.
     * 예: "Dense pine forest with moonlight filtering through the mist"
     */
    private String backgroundDetail;

    /** 조명·분위기 (예: "dark storm clouds, dramatic lightning") */
    private String lighting;

    /**
     * 씬에 대한 구체적 설명/사건 (2~3문장)
     * 기존 mainStory 필드를 대체한다.
     */
    private String majorStory;

    /**
     * 첫 프레임 이미지 생성 영어 프롬프트
     * FAL.ai FLUX Kontext 호출 시 첫 번째 프레임 이미지 생성에 사용된다.
     */
    private String firstFramePrompt;

    /**
     * 첫 프레임 이미지 URL
     * 플롯 생성 직후에는 null이며, 프레임 재생성 API(POST .../frames/regenerate)로 채워진다.
     */
    private String firstFrameImageUrl;

    /**
     * 마지막 프레임 이미지 생성 영어 프롬프트
     * FAL.ai FLUX Kontext 호출 시 마지막 프레임 이미지 생성에 사용된다.
     */
    private String lastFramePrompt;

    /**
     * 마지막 프레임 이미지 URL
     * 플롯 생성 직후에는 null이며, 프레임 재생성 API(POST .../frames/regenerate)로 채워진다.
     */
    private String lastFrameImageUrl;
}
