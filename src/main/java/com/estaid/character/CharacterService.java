package com.estaid.character;

import com.estaid.character.dto.CharacterRequest;
import com.estaid.character.dto.CharacterResponse;
import com.estaid.common.exception.BusinessException;
import com.estaid.project.Project;
import com.estaid.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 캐릭터 비즈니스 로직 서비스
 *
 * <p>캐릭터 생성·조회·수정·삭제(CRUD)를 담당한다.
 * 캐릭터는 반드시 특정 프로젝트에 속해야 하므로,
 * 생성·수정 시 projectId로 프로젝트 존재 여부를 먼저 검증한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final ProjectRepository projectRepository;

    /**
     * 특정 프로젝트의 캐릭터 목록 조회
     *
     * @param projectId 프로젝트 UUID
     * @return 해당 프로젝트에 속한 캐릭터 목록
     * @throws BusinessException 프로젝트가 존재하지 않을 때 (404)
     */
    @Transactional(readOnly = true)
    public List<CharacterResponse> findAllByProject(String projectId) {
        getProjectOrThrow(projectId); // 프로젝트 존재 여부 검증
        return characterRepository.findByProject_ProjectId(projectId)
                .stream()
                .map(CharacterResponse::from)
                .toList();
    }

    /**
     * 캐릭터 단건 조회
     *
     * @param characterId 조회할 캐릭터 UUID
     * @return 캐릭터 응답 DTO
     * @throws BusinessException 캐릭터가 존재하지 않을 때 (404)
     */
    @Transactional(readOnly = true)
    public CharacterResponse findById(String characterId) {
        return CharacterResponse.from(getCharacterOrThrow(characterId));
    }

    /**
     * 캐릭터 생성
     *
     * <p>요청의 projectId로 프로젝트를 조회한 뒤,
     * 해당 프로젝트에 속한 캐릭터를 생성하고 저장한다.</p>
     *
     * @param request 캐릭터 생성 요청 DTO
     * @return 생성된 캐릭터 응답 DTO
     * @throws BusinessException 프로젝트가 존재하지 않을 때 (404)
     */
    @Transactional
    public CharacterResponse create(CharacterRequest request) {
        Project project = getProjectOrThrow(request.getProjectId());

        Character character = Character.builder()
                .project(project)
                .name(request.getName())
                .description(request.getDescription())
                .referenceImageUrl(request.getReferenceImageUrl())
                .artStyle(request.getArtStyle())
                .build();

        Character saved = characterRepository.save(character);
        log.info("캐릭터 생성 완료: characterId={}, name={}, projectId={}",
                saved.getCharacterId(), saved.getName(), request.getProjectId());
        return CharacterResponse.from(saved);
    }

    /**
     * 캐릭터 수정
     *
     * <p>전달된 필드로 기존 값을 덮어쓴다.
     * null인 필드도 그대로 반영되므로, 클라이언트는 수정하지 않는 필드도 기존 값을 함께 전송해야 한다.</p>
     *
     * @param characterId 수정할 캐릭터 UUID
     * @param request     수정 요청 DTO
     * @return 수정된 캐릭터 응답 DTO
     * @throws BusinessException 캐릭터 또는 프로젝트가 존재하지 않을 때 (404)
     */
    @Transactional
    public CharacterResponse update(String characterId, CharacterRequest request) {
        Character character = getCharacterOrThrow(characterId);
        Project project = getProjectOrThrow(request.getProjectId());

        character.setProject(project);
        character.setName(request.getName());
        character.setDescription(request.getDescription());
        character.setReferenceImageUrl(request.getReferenceImageUrl());
        character.setArtStyle(request.getArtStyle());

        log.info("캐릭터 수정 완료: characterId={}", characterId);
        return CharacterResponse.from(character);
    }

    /**
     * 캐릭터 삭제
     *
     * <p>DB에 ON DELETE SET NULL이 설정되어 있어
     * 캐릭터 삭제 시 해당 캐릭터를 참조하는 플롯의 character_id는 NULL로 설정된다.</p>
     *
     * @param characterId 삭제할 캐릭터 UUID
     * @throws BusinessException 캐릭터가 존재하지 않을 때 (404)
     */
    @Transactional
    public void delete(String characterId) {
        Character character = getCharacterOrThrow(characterId);
        characterRepository.delete(character);
        log.info("캐릭터 삭제 완료: characterId={}", characterId);
    }

    /**
     * 캐릭터 조회 공통 메서드 - 없으면 예외 발생
     *
     * @param characterId 조회할 캐릭터 UUID
     * @return 캐릭터 엔티티
     * @throws BusinessException 캐릭터가 존재하지 않을 때 (404)
     */
    private Character getCharacterOrThrow(String characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new BusinessException(
                        "캐릭터를 찾을 수 없습니다. id=" + characterId,
                        HttpStatus.NOT_FOUND
                ));
    }

    /**
     * 프로젝트 조회 공통 메서드 - 없으면 예외 발생
     *
     * @param projectId 조회할 프로젝트 UUID
     * @return 프로젝트 엔티티
     * @throws BusinessException 프로젝트가 존재하지 않을 때 (404)
     */
    private Project getProjectOrThrow(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        "프로젝트를 찾을 수 없습니다. id=" + projectId,
                        HttpStatus.NOT_FOUND
                ));
    }
}
