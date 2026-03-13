package com.estaid.character;

import com.estaid.character.dto.CharacterRequest;
import com.estaid.character.dto.CharacterResponse;
import com.estaid.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 캐릭터 REST 컨트롤러
 *
 * <p>기본 경로: {@code /api/projects/{projectId}/characters}</p>
 *
 * <ul>
 *   <li>GET    /api/projects/{projectId}/characters              - 프로젝트 내 전체 목록 조회</li>
 *   <li>GET    /api/projects/{projectId}/characters/{id}         - 단건 조회</li>
 *   <li>POST   /api/projects/{projectId}/characters              - 생성</li>
 *   <li>PUT    /api/projects/{projectId}/characters/{id}         - 수정</li>
 *   <li>DELETE /api/projects/{projectId}/characters/{id}         - 삭제</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/projects/{projectId}/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    /**
     * 프로젝트 내 캐릭터 전체 목록 조회
     *
     * @param projectId 프로젝트 UUID (경로 변수)
     * @return 200 OK + 캐릭터 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CharacterResponse>>> findAll(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.ok(characterService.findAllByProject(projectId)));
    }

    /**
     * 캐릭터 단건 조회
     *
     * @param characterId 조회할 캐릭터 UUID (경로 변수)
     * @return 200 OK + 캐릭터 정보
     */
    @GetMapping("/{characterId}")
    public ResponseEntity<ApiResponse<CharacterResponse>> findById(@PathVariable String characterId) {
        return ResponseEntity.ok(ApiResponse.ok(characterService.findById(characterId)));
    }

    /**
     * 캐릭터 생성
     *
     * @param projectId 소속 프로젝트 UUID (경로 변수)
     * @param request   생성 요청 바디 (projectId, name 필수)
     * @return 201 Created + 생성된 캐릭터 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CharacterResponse>> create(
            @PathVariable String projectId,
            @Valid @RequestBody CharacterRequest request) {
        CharacterResponse response = characterService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("캐릭터가 생성되었습니다.", response));
    }

    /**
     * 캐릭터 수정
     *
     * @param characterId 수정할 캐릭터 UUID (경로 변수)
     * @param request     수정 요청 바디
     * @return 200 OK + 수정된 캐릭터 정보
     */
    @PutMapping("/{characterId}")
    public ResponseEntity<ApiResponse<CharacterResponse>> update(
            @PathVariable String characterId,
            @Valid @RequestBody CharacterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("캐릭터가 수정되었습니다.", characterService.update(characterId, request)));
    }

    /**
     * 캐릭터 삭제
     *
     * <p>삭제 시 해당 캐릭터를 참조하는 플롯의 character_id는 NULL로 설정된다.</p>
     *
     * @param characterId 삭제할 캐릭터 UUID (경로 변수)
     * @return 200 OK
     */
    @DeleteMapping("/{characterId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String characterId) {
        characterService.delete(characterId);
        return ResponseEntity.ok(ApiResponse.ok("캐릭터가 삭제되었습니다.", null));
    }
}
