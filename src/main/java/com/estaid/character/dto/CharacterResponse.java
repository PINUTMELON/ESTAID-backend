package com.estaid.character.dto;

import com.estaid.character.Character;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * 캐릭터 응답 DTO
 *
 * <p>{@link Character} 엔티티를 직접 노출하지 않고
 * 필요한 필드만 골라 클라이언트에 전달한다.</p>
 */
@Getter
@Builder
public class CharacterResponse {

    /** 캐릭터 고유 식별자 (UUID) */
    private String characterId;

    /** 소속 프로젝트 UUID */
    private String projectId;

    /** 캐릭터 이름 */
    private String name;

    /** 캐릭터 외형·성격·특징 설명 */
    private String description;

    /** 레퍼런스 이미지 URL */
    private String referenceImageUrl;

    /** 화풍 설정 (anime, realistic, webtoon 등) */
    private String artStyle;

    /** 생성 시각 */
    private OffsetDateTime createdAt;

    /** 최종 수정 시각 */
    private OffsetDateTime updatedAt;

    /**
     * {@link Character} 엔티티를 {@link CharacterResponse} DTO로 변환한다.
     *
     * @param character 변환할 캐릭터 엔티티
     * @return CharacterResponse DTO
     */
    public static CharacterResponse from(Character character) {
        return CharacterResponse.builder()
                .characterId(character.getCharacterId())
                .projectId(character.getProject() != null ? character.getProject().getProjectId() : null)
                .name(character.getName())
                .description(character.getDescription())
                .referenceImageUrl(character.getReferenceImageUrl())
                .artStyle(character.getArtStyle())
                .createdAt(character.getCreatedAt())
                .updatedAt(character.getUpdatedAt())
                .build();
    }
}
