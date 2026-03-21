package com.estaid.asset;

import com.estaid.asset.dto.AssetResponse;
import com.estaid.asset.dto.AssetSaveRequest;
import com.estaid.asset.dto.GenerateRequest;
import com.estaid.asset.dto.GenerateResponse;
import com.estaid.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Asset REST 컨트롤러
 *
 * <p>캐릭터·배경 이미지 임시 생성 및 프로젝트 저장을 담당한다.</p>
 *
 * <ul>
 *   <li>POST /api/characters/generate              - 캐릭터 이미지 임시 생성 (DB 저장 X)</li>
 *   <li>POST /api/backgrounds/generate             - 배경 이미지 임시 생성 (DB 저장 X)</li>
 *   <li>POST /api/projects/{projectId}/assets      - "프로젝트에 사용하기" 확정 저장</li>
 *   <li>GET  /api/projects/{projectId}/assets      - 프로젝트의 Asset 목록 조회</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    /**
     * 캐릭터 이미지 임시 생성
     *
     * <p>FAL.ai FLUX Kontext로 캐릭터 이미지를 생성하여 URL을 반환한다.
     * DB에 저장하지 않으며, 마음에 들면 {@code POST /api/projects/{id}/assets}로 저장한다.</p>
     *
     * @param request 생성 요청 (prompt 필수, style·referenceImageUrl 선택)
     * @return 200 OK + 생성된 이미지 URL
     */
    @PostMapping("/api/characters/generate")
    public ResponseEntity<ApiResponse<GenerateResponse>> generateCharacterImage(
            @Valid @RequestBody GenerateRequest request) {
        GenerateResponse response = assetService.generateImage(request);
        return ResponseEntity.ok(ApiResponse.ok("캐릭터 이미지가 생성되었습니다.", response));
    }

    /**
     * 배경 이미지 임시 생성
     *
     * <p>FAL.ai FLUX Kontext로 배경 이미지를 생성하여 URL을 반환한다.
     * DB에 저장하지 않으며, 마음에 들면 {@code POST /api/projects/{id}/assets}로 저장한다.</p>
     *
     * @param request 생성 요청 (prompt 필수, style 선택)
     * @return 200 OK + 생성된 이미지 URL
     */
    @PostMapping("/api/backgrounds/generate")
    public ResponseEntity<ApiResponse<GenerateResponse>> generateBackgroundImage(
            @Valid @RequestBody GenerateRequest request) {
        GenerateResponse response = assetService.generateImage(request);
        return ResponseEntity.ok(ApiResponse.ok("배경 이미지가 생성되었습니다.", response));
    }

    /**
     * "프로젝트에 사용하기" - Asset 확정 저장
     *
     * <p>임시 생성된 이미지를 프로젝트에 확정 저장한다.
     * type에 따라 CHARACTER 또는 BACKGROUND로 분류된다.</p>
     *
     * @param projectId 저장할 프로젝트 UUID
     * @param request   저장 요청 (type, imageUrl 필수)
     * @return 201 Created + 저장된 Asset 정보
     */
    @PostMapping("/api/projects/{projectId}/assets")
    public ResponseEntity<ApiResponse<AssetResponse>> saveAsset(
            @PathVariable String projectId,
            @Valid @RequestBody AssetSaveRequest request) {
        AssetResponse response = assetService.saveAsset(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("프로젝트에 저장되었습니다.", response));
    }

    /**
     * 프로젝트의 Asset 목록 조회
     *
     * <p>프로젝트 페이지에서 사용된 캐릭터·배경 이미지를 조회한다.</p>
     *
     * @param projectId 조회할 프로젝트 UUID
     * @return 200 OK + Asset 목록 (생성 시각 오름차순)
     */
    @GetMapping("/api/projects/{projectId}/assets")
    public ResponseEntity<ApiResponse<List<AssetResponse>>> findAllByProject(
            @PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.findAllByProject(projectId)));
    }
}
