package com.estaid.plot;

import com.estaid.common.response.ApiResponse;
import com.estaid.plot.dto.FrameRegenerateRequest;
import com.estaid.plot.dto.FrameRegenerateResponse;
import com.estaid.plot.dto.PlotCreateRequest;
import com.estaid.plot.dto.PlotGenerateRequest;
import com.estaid.plot.dto.PlotResponse;
import com.estaid.plot.dto.SceneBatchSaveRequest;
import com.estaid.plot.dto.SceneUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 플롯 REST 컨트롤러
 *
 * <p>기존 경로: {@code /api/plots} (하위 호환 유지)</p>
 *
 * <ul>
 *   <li>POST   /api/plots                                      - 플롯 생성 (기존, 하위 호환)</li>
 *   <li>GET    /api/plots?projectId={id}                       - 프로젝트 내 플롯 목록 조회</li>
 *   <li>GET    /api/plots/{plotId}                             - 플롯 단건 조회</li>
 *   <li>PUT    /api/plots/{plotId}/scenes/{sceneNumber}        - 특정 씬 단건 수정 (하위 호환)</li>
 *   <li>DELETE /api/plots/{plotId}                             - 플롯 삭제</li>
 * </ul>
 *
 * <p>신규 경로: {@code /api/projects}</p>
 *
 * <ul>
 *   <li>POST /api/projects/plots/generate                      - AI 스토리보드 자동 생성 (한 프로젝트당 1회)</li>
 *   <li>PUT  /api/projects/{projectId}/plots/scenes            - 전체 씬 배치 저장 (다음 버튼)</li>
 *   <li>POST /api/projects/plots/frames/regenerate             - 특정 프레임 이미지 재생성</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class PlotController {

    private final PlotService plotService;

    // ─────────────────────────────────────────
    // 기존 API (하위 호환 유지)
    // ─────────────────────────────────────────

    /**
     * 플롯 생성 (기존 API, 하위 호환 유지)
     *
     * <p>Claude API를 호출하여 {@code sceneCount}개의 씬을 자동 생성한다.
     * Claude API 응답 대기 시간이 있어 최대 60초 소요될 수 있다.</p>
     *
     * @param request 플롯 생성 요청 바디 (projectId, title, idea, sceneCount 등)
     * @return 201 Created + 생성된 플롯 정보 (씬 목록 포함)
     */
    @PostMapping("/api/plots")
    public ResponseEntity<ApiResponse<PlotResponse>> create(@Valid @RequestBody PlotCreateRequest request) {
        PlotResponse response = plotService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("플롯이 생성되었습니다.", response));
    }

    // ─────────────────────────────────────────
    // 신규 API
    // ─────────────────────────────────────────

    /**
     * AI 스토리보드 자동 생성 (신규 — 한 프로젝트당 1번만 가능)
     *
     * <p>전체 줄거리를 입력받아 Claude AI가 sceneCount개의 씬을 자동 생성한다.
     * 이미 플롯이 존재하면 409 Conflict를 반환한다.</p>
     *
     * <p>composition 필드는 Enum 10가지 값 중 하나로 반환된다.
     * firstFrameImageUrl / lastFrameImageUrl은 생성 직후 null이며,
     * {@code POST /api/projects/plots/frames/regenerate}로 개별 생성한다.</p>
     *
     * @param request 스토리보드 생성 요청 바디 (projectId, storyDescription, sceneCount, ratio)
     * @return 201 Created + 생성된 플롯 정보 (씬 목록 포함)
     */
    @PostMapping("/api/projects/plots/generate")
    public ResponseEntity<ApiResponse<PlotResponse>> generate(@Valid @RequestBody PlotGenerateRequest request) {
        PlotResponse response = plotService.generate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("스토리보드가 생성되었습니다.", response));
    }

    /**
     * 전체 씬 배치 저장 (다음 버튼 클릭 시 호출)
     *
     * <p>씬 만들기 페이지에서 사용자가 수정한 전체 씬 목록을 한 번에 저장한다.
     * 기존 scenesJson을 요청 데이터로 완전히 교체한다.</p>
     *
     * @param projectId 프로젝트 UUID (경로 변수)
     * @param request   씬 목록 배치 저장 요청 바디
     * @return 200 OK
     */
    @PutMapping("/api/projects/{projectId}/plots/scenes")
    public ResponseEntity<ApiResponse<Void>> saveScenesBatch(
            @PathVariable String projectId,
            @Valid @RequestBody SceneBatchSaveRequest request) {
        plotService.saveScenesBatch(projectId, request);
        return ResponseEntity.ok(ApiResponse.ok("씬이 저장되었습니다.", null));
    }

    /**
     * 프레임 이미지 재생성
     *
     * <p>씬의 첫 프레임 또는 마지막 프레임 이미지를 FAL.ai로 재생성한다.
     * 재생성 후 SceneDto의 imageUrl과 prompt가 업데이트된다.</p>
     *
     * @param request 프레임 재생성 요청 바디 (projectId, sceneNumber, firstOrLast, prompt)
     * @return 200 OK + 재생성된 이미지 URL
     */
    @PostMapping("/api/projects/plots/frames/regenerate")
    public ResponseEntity<ApiResponse<FrameRegenerateResponse>> regenerateFrame(
            @Valid @RequestBody FrameRegenerateRequest request) {
        FrameRegenerateResponse response = plotService.regenerateFrame(request);
        return ResponseEntity.ok(ApiResponse.ok("프레임 이미지가 재생성되었습니다.", response));
    }

    /**
     * 프로젝트 내 플롯 목록 조회 (기존 API, 하위 호환)
     *
     * @param projectId 조회할 프로젝트 UUID (쿼리 파라미터)
     * @return 200 OK + 플롯 목록 (씬 목록 포함)
     */
    @GetMapping("/api/plots")
    public ResponseEntity<ApiResponse<List<PlotResponse>>> findAllByProject(
            @RequestParam String projectId) {
        return ResponseEntity.ok(ApiResponse.ok(plotService.findAllByProject(projectId)));
    }

    /**
     * 플롯 단건 조회 (기존 API, 하위 호환)
     *
     * @param plotId 조회할 플롯 UUID
     * @return 200 OK + 플롯 정보 (씬 목록 포함)
     */
    @GetMapping("/api/plots/{plotId}")
    public ResponseEntity<ApiResponse<PlotResponse>> findById(@PathVariable String plotId) {
        return ResponseEntity.ok(ApiResponse.ok(plotService.findById(plotId)));
    }

    /**
     * 특정 씬 단건 수정 (기존 API, 하위 호환)
     *
     * <p>Claude가 생성한 씬 내용을 프론트엔드 표(테이블)에서 수정한 후 호출한다.
     * 수정된 씬은 DB의 {@code scenes_json}에 반영된다.</p>
     *
     * @param plotId      수정할 플롯 UUID
     * @param sceneNumber 수정할 씬 번호 (1부터 시작)
     * @param request     씬 수정 요청 바디
     * @return 200 OK + 수정된 플롯 전체 정보 (씬 목록 포함)
     */
    @PutMapping("/api/plots/{plotId}/scenes/{sceneNumber}")
    public ResponseEntity<ApiResponse<PlotResponse>> updateScene(
            @PathVariable String plotId,
            @PathVariable int sceneNumber,
            @RequestBody SceneUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("씬이 수정되었습니다.", plotService.updateScene(plotId, sceneNumber, request)));
    }

    /**
     * 플롯 삭제 (기존 API, 하위 호환)
     *
     * <p>연결된 이미지·영상도 CASCADE 삭제된다.</p>
     *
     * @param plotId 삭제할 플롯 UUID
     * @return 200 OK
     */
    @DeleteMapping("/api/plots/{plotId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String plotId) {
        plotService.delete(plotId);
        return ResponseEntity.ok(ApiResponse.ok("플롯이 삭제되었습니다.", null));
    }
}
