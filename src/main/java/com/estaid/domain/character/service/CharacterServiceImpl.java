package com.estaid.domain.character.service;

import com.estaid.common.exception.BusinessException;
import com.estaid.domain.character.dto.CharacterCreateRequest;
import com.estaid.domain.character.dto.CharacterResponse;
import com.estaid.domain.character.entity.Character;
import com.estaid.domain.character.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 캐릭터 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterServiceImpl implements CharacterService {

    private final CharacterRepository characterRepository;

    /**
     * 캐릭터 등록
     * - 요청 정보를 Character 엔티티로 변환하여 DB에 저장한다.
     * - referenceImageUrl은 추후 외부 스토리지(S3 등) URL로 교체 가능하다.
     */
    @Transactional
    @Override
    public CharacterResponse createCharacter(CharacterCreateRequest request) {
        log.info("캐릭터 등록 요청: {}", request.getName());

        Character character = Character.builder()
                .name(request.getName())
                .description(request.getDescription())
                .referenceImageUrl(request.getReferenceImageUrl())
                .artStyle(request.getArtStyle())
                .build();

        Character saved = characterRepository.save(character);
        return CharacterResponse.from(saved);
    }

    /**
     * 캐릭터 단건 조회
     * - 존재하지 않으면 404 예외를 던진다.
     */
    @Transactional(readOnly = true)
    @Override
    public CharacterResponse getCharacter(String characterId) {
        log.info("캐릭터 조회: {}", characterId);

        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new BusinessException("캐릭터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        return CharacterResponse.from(character);
    }
}
