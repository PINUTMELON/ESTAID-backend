package com.estaid.domain.character.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * 캐릭터 생성 요청 DTO
 */
@Getter
public class CharacterCreateRequest {

    /** 캐릭터 이름 (필수) */
    @NotBlank(message = "캐릭터 이름은 필수입니다.")
    private String name;

    /** 캐릭터 설명 (외형, 성격 등) */
    private String description;

    /** 레퍼런스 이미지 URL */
    private String referenceImageUrl;

    /** 화풍 (예: anime, realistic, webtoon) */
    private String artStyle;
}
