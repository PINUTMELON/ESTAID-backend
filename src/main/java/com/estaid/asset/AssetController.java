package com.estaid.asset;

import com.estaid.asset.dto.AssetCreateResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Asset REST 컨트롤러
 *
 * <p>캐릭터·배경 이미지 임시 생성 및 프로젝트 저장을 담당한다.</p>
 *
 * <p>기존 API (하위 호환 유지):</p>
 * <ul>
 *   <li>POST /api/characters/generate              - 캐릭터 이미지 임시 생성 (DB 저장 X)</li>
 *   <li>POST /api/backgrounds/generate             - 배경 이미지 임시 생성 (DB 저장 X)</li>
 *   <li>POST /api/projects/{projectId}/assets      - "프로젝트에 사용하기" 확정 저장</li>
 *   <li>GET  /api/projects/{projectId}/assets      - 프로젝트의 Asset 목록 조회</li>
 * </ul>
 *
 * <p>신규 API (통합 생성/삭제):</p>
 * <ul>
 *   <li>POST   /api/projects/{projectId}/{assetType}            - 1단계 통합 생성 (Multipart)</li>
 *   <li>DELETE /api/projects/{projectId}/{assetType}/{assetId}  - 자산 개별 삭제</li>
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

    // ─────────────────────────────────────────
    // 신규 API: 통합 생성 / 개별 삭제
    // ─────────────────────────────────────────

    /**
     * 자산 1단계 통합 생성 (Multipart)
     *
     * <p>참조 이미지 업로드 → AI 이미지 생성 → 엔티티 저장을 한 번에 처리한다.
     * 기존 2단계 방식(generate → save)을 대체하는 신규 API이다.</p>
     *
     * <p>assetType 경로 변수:</p>
     * <ul>
     *   <li>{@code characters}  - 캐릭터 자산 생성 → CharacterEntity + Asset 저장</li>
     *   <li>{@code backgrounds} - 배경 자산 생성   → BackgroundEntity + Asset 저장</li>
     * </ul>
     *
     * <p>Multipart 필드:</p>
     * <ul>
     *   <li>name           - 자산 이름 (필수)</li>
     *   <li>referenceImage - 참조 이미지 파일 (필수, image/*)</li>
     *   <li>style          - 화풍 (필수, 예: REALISTIC, ANIME, 3D, PAINT, SKETCH)</li>
     *   <li>ratio          - 이미지 비율 (선택, 예: 16:9, 9:16, 1:1, 4:3)</li>
     *   <li>quality        - 이미지 품질 (선택, 예: Standard, High)</li>
     * </ul>
     *
     * @param projectId      소속 프로젝트 UUID (경로 변수)
     * @param assetType      자산 유형 ("characters" 또는 "backgrounds", 경로 변수)
     * @param name           자산 이름 (Multipart 필드, 필수)
     * @param referenceImage 참조 이미지 파일 (Multipart 파트, 필수)
     * @param style          화풍 (Multipart 필드, 필수)
     * @param ratio          이미지 비율 (Multipart 필드, 선택)
     * @param quality        이미지 품질 (Multipart 필드, 선택)
     * @return 201 Created + 생성된 자산 정보 (id, name, referenceImageUrl, imageUrl, status)
     */
    @PostMapping("/api/projects/{projectId}/{assetType}")
    public ResponseEntity<ApiResponse<AssetCreateResponse>> createAsset(
            @PathVariable String projectId,
            @PathVariable String assetType,
            @RequestParam("name") String name,
            @RequestPart("referenceImage") MultipartFile referenceImage,
            @RequestParam("style") String style,
            @RequestParam(value = "ratio", required = false) String ratio,
            @RequestParam(value = "quality", required = false) String quality) {

        AssetCreateResponse response = assetService.createAsset(
                projectId, assetType, name, referenceImage, style, ratio, quality);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("자산이 생성되었습니다.", response));
    }

    /**
     * 자산 개별 삭제
     *
     * <p>캐릭터 또는 배경 엔티티를 삭제한다.
     * assetType에 따라 캐릭터(characterId) 또는 배경(backgroundId)을 삭제한다.</p>
     *
     * <p>assetType 경로 변수:</p>
     * <ul>
     *   <li>{@code characters}  - CharacterEntity 삭제</li>
     *   <li>{@code backgrounds} - BackgroundEntity 삭제</li>
     * </ul>
     *
     * @param projectId 소속 프로젝트 UUID (경로 변수)
     * @param assetType 자산 유형 ("characters" 또는 "backgrounds", 경로 변수)
     * @param assetId   삭제할 자산 UUID (경로 변수)
     * @return 200 OK
     */
    @DeleteMapping("/api/projects/{projectId}/{assetType}/{assetId}")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(
            @PathVariable String projectId,
            @PathVariable String assetType,
            @PathVariable String assetId) {

        assetService.deleteAsset(projectId, assetType, assetId);
        return ResponseEntity.ok(ApiResponse.ok("자산이 삭제되었습니다.", null));
    }
}
