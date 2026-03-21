package com.estaid.character.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 캐릭터 생성/수정 요청 DTO
 *
 * <ul>
 *   <li>생성: POST /api/projects/{projectId}/characters</li>
 *   <li>수정: PUT  /api/projects/{projectId}/characters/{characterId}</li>
 * </ul>
 */
@Getter
@NoArgsConstructor
public class CharacterRequest {

    /**
     * 소속 프로젝트 UUID (필수)
     * URL 경로의 projectId와 동일하게 전달한다.
     */
    @NotBlank(message = "프로젝트 ID는 필수입니다.")
    private String projectId;

    /**
     * 캐릭터 이름 (필수, 최대 100자)
     * 예: "강백호", "나루토", "아이유"
     */
    @NotBlank(message = "캐릭터 이름은 필수입니다.")
    @Size(max = 100, message = "캐릭터 이름은 100자 이하로 입력해주세요.")
    private String name;

    /**
     * 캐릭터 외형·성격·특징 상세 설명 (선택)
     * Claude API 씬 생성 프롬프트에 포함되어 캐릭터 묘사에 활용된다.
     * 예: "20대 초반 여성, 긴 갈색 머리, 청순하고 밝은 이미지"
     */
    private String description;

    /**
     * 레퍼런스 이미지 URL (선택)
     * FAL.ai FLUX Kontext 호출 시 image_url로 전달되어
     * 씬 간 캐릭터 외형 일관성(얼굴+옷+체형)을 유지한다.
     */
    private String referenceImageUrl;

    /**
     * 화풍 설정 (선택)
     * 예: anime, realistic, webtoon
     * 이미지 생성 프롬프트에 "style: {artStyle}" 형태로 삽입된다.
     */
    private String artStyle;
}
