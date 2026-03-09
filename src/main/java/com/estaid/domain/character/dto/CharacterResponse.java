package com.estaid.domain.character.dto;

import com.estaid.domain.character.entity.Character;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 캐릭터 응답 DTO
 */
@Getter
@Builder
public class CharacterResponse {

    private String characterId;
    private String name;
    private String description;
    private String referenceImageUrl;
    private String artStyle;
    private LocalDateTime createdAt;

    /** 엔티티 → DTO 변환 */
    public static CharacterResponse from(Character character) {
        return CharacterResponse.builder()
                .characterId(character.getCharacterId())
                .name(character.getName())
                .description(character.getDescription())
                .referenceImageUrl(character.getReferenceImageUrl())
                .artStyle(character.getArtStyle())
                .createdAt(character.getCreatedAt())
                .build();
    }
}
