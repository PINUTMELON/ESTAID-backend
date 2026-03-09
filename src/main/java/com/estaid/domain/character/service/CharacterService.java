package com.estaid.domain.character.service;

import com.estaid.domain.character.dto.CharacterCreateRequest;
import com.estaid.domain.character.dto.CharacterResponse;

/**
 * 캐릭터 서비스 인터페이스
 */
public interface CharacterService {

    /** 캐릭터 등록 */
    CharacterResponse createCharacter(CharacterCreateRequest request);

    /** 캐릭터 단건 조회 */
    CharacterResponse getCharacter(String characterId);
}
