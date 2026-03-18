package com.estaid.plot.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * 플롯 생성 요청 DTO
 *
 * <p>사용자가 스토리 아이디어와 씬 수를 입력하면,
 * Claude API를 호출하여 {@code sceneCount}개의 씬을 자동 생성한다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlotCreateRequest {

    /**
     * 소속 프로젝트 UUID (필수)
     * 해당 프로젝트 아래에 플롯이 생성된다.
     */
    @NotBlank(message = "프로젝트 ID는 필수입니다.")
    private String projectId;

    /**
     * 플롯 제목 (필수, 최대 200자)
     * 예: "괴물과의 대결", "벚꽃 공원에서의 만남"
     */
    @NotBlank(message = "플롯 제목은 필수입니다.")
    @Size(max = 200, message = "플롯 제목은 200자를 초과할 수 없습니다.")
    private String title;

    /**
     * 사용자가 입력한 스토리 아이디어 (필수)
     * Claude API 씬 생성 프롬프트의 핵심 입력값이다.
     * 예: "주인공이 도심 폐허에서 거대 괴물과 맞닥뜨려 싸우는 장면"
     */
    @NotBlank(message = "스토리 아이디어는 필수입니다.")
    private String idea;

    /**
     * 생성할 씬 수 (선택, 기본값 5, 최소 1 ~ 최대 10)
     * Claude API 호출 시 이 수만큼의 씬 JSON을 생성하도록 지시한다.
     */
    @Min(value = 1, message = "씬 수는 1 이상이어야 합니다.")
    @Max(value = 10, message = "씬 수는 10 이하이어야 합니다.")
    private Integer sceneCount = 5;

    /**
     * 화풍 설정 (선택)
     * 이미지 생성 프롬프트에 일관되게 적용된다.
     * 예: anime, realistic, webtoon
     */
    @Size(max = 50, message = "아트 스타일은 50자를 초과할 수 없습니다.")
    private String artStyle;

    /**
     * 참조 캐릭터 UUID (선택)
     * 설정 시 Claude 씬 생성 프롬프트에 캐릭터 이름·설명이 포함되어
     * 씬 내 캐릭터 묘사의 일관성이 높아진다.
     */
    private String characterId;
}
