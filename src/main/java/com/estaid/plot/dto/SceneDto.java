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
 *   <li>sceneNumber  - 씬 순서 (1부터 시작)</li>
 *   <li>title        - 씬 제목</li>
 *   <li>characters   - 등장인물</li>
 *   <li>composition  - 카메라 구도 (ex. wide shot, close-up)</li>
 *   <li>background   - 배경 묘사</li>
 *   <li>lighting     - 조명·분위기</li>
 *   <li>mainStory    - 주요 스토리 (2~3문장)</li>
 *   <li>firstFramePrompt  - 첫 프레임 이미지 생성 영어 프롬프트</li>
 *   <li>lastFramePrompt   - 마지막 프레임 이미지 생성 영어 프롬프트</li>
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

    /** 카메라 구도 (예: "wide cinematic shot", "close-up", "low angle") */
    private String composition;

    /** 배경 묘사 (예: "폐허가 된 도심, 먼지와 잔해") */
    private String background;

    /** 조명·분위기 (예: "dark storm clouds, dramatic lightning") */
    private String lighting;

    /** 주요 스토리 요약 (2~3문장) */
    private String mainStory;

    /**
     * 첫 프레임 이미지 생성 영어 프롬프트
     * FAL.ai FLUX Kontext 호출 시 첫 번째 프레임 이미지 생성에 사용된다.
     */
    private String firstFramePrompt;

    /**
     * 마지막 프레임 이미지 생성 영어 프롬프트
     * FAL.ai FLUX Kontext 호출 시 마지막 프레임 이미지 생성에 사용된다.
     */
    private String lastFramePrompt;
}
