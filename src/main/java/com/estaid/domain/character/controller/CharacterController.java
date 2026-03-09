package com.estaid.domain.character.controller;

import com.estaid.common.response.ApiResponse;
import com.estaid.domain.character.dto.CharacterCreateRequest;
import com.estaid.domain.character.dto.CharacterResponse;
import com.estaid.domain.character.service.CharacterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 캐릭터 컨트롤러
 *
 * POST /api/characters         - 캐릭터 등록
 * GET  /api/characters/{id}    - 캐릭터 조회
 */
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    /** 캐릭터 등록 */
    @PostMapping
    public ResponseEntity<ApiResponse<CharacterResponse>> createCharacter(
            @Valid @RequestBody CharacterCreateRequest request) {

        CharacterResponse response = characterService.createCharacter(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("캐릭터가 등록되었습니다.", response));
    }

    /** 캐릭터 단건 조회 */
    @GetMapping("/{characterId}")
    public ResponseEntity<ApiResponse<CharacterResponse>> getCharacter(
            @PathVariable String characterId) {

        CharacterResponse response = characterService.getCharacter(characterId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
