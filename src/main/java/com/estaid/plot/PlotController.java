package com.estaid.plot;

import com.estaid.common.response.ApiResponse;
import com.estaid.plot.dto.PlotCreateRequest;
import com.estaid.plot.dto.PlotResponse;
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
 * <p>기본 경로: {@code /api/plots}</p>
 *
 * <ul>
 *   <li>POST   /api/plots                                  - 플롯 생성 (Claude AI 씬 자동 생성)</li>
 *   <li>GET    /api/plots?projectId={id}                   - 프로젝트 내 플롯 목록 조회</li>
 *   <li>GET    /api/plots/{plotId}                         - 플롯 단건 조회</li>
 *   <li>PUT    /api/plots/{plotId}/scenes/{sceneNumber}    - 특정 씬 내용 수정</li>
 *   <li>DELETE /api/plots/{plotId}                         - 플롯 삭제</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/plots")
@RequiredArgsConstructor
public class PlotController {

    private final PlotService plotService;

    /**
     * 플롯 생성 (Claude AI 씬 자동 생성)
     *
     * <p>Claude API를 호출하여 {@code sceneCount}개의 씬을 자동 생성한다.
     * Claude API 응답 대기 시간이 있어 최대 60초 소요될 수 있다.</p>
     *
     * @param request 플롯 생성 요청 바디 (projectId, title, idea, sceneCount 등)
     * @return 201 Created + 생성된 플롯 정보 (씬 목록 포함)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PlotResponse>> create(@Valid @RequestBody PlotCreateRequest request) {
        PlotResponse response = plotService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("플롯이 생성되었습니다.", response));
    }

    /**
     * 프로젝트 내 플롯 목록 조회
     *
     * @param projectId 조회할 프로젝트 UUID (쿼리 파라미터)
     * @return 200 OK + 플롯 목록 (씬 목록 포함)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlotResponse>>> findAllByProject(
            @RequestParam String projectId) {
        return ResponseEntity.ok(ApiResponse.ok(plotService.findAllByProject(projectId)));
    }

    /**
     * 플롯 단건 조회
     *
     * @param plotId 조회할 플롯 UUID
     * @return 200 OK + 플롯 정보 (씬 목록 포함)
     */
    @GetMapping("/{plotId}")
    public ResponseEntity<ApiResponse<PlotResponse>> findById(@PathVariable String plotId) {
        return ResponseEntity.ok(ApiResponse.ok(plotService.findById(plotId)));
    }

    /**
     * 특정 씬 내용 수정 (사용자 직접 편집)
     *
     * <p>Claude가 생성한 씬 내용을 프론트엔드 표(테이블)에서 수정한 후 호출한다.
     * 수정된 씬은 DB의 {@code scenes_json}에 반영된다.</p>
     *
     * @param plotId      수정할 플롯 UUID
     * @param sceneNumber 수정할 씬 번호 (1부터 시작)
     * @param request     씬 수정 요청 바디
     * @return 200 OK + 수정된 플롯 전체 정보 (씬 목록 포함)
     */
    @PutMapping("/{plotId}/scenes/{sceneNumber}")
    public ResponseEntity<ApiResponse<PlotResponse>> updateScene(
            @PathVariable String plotId,
            @PathVariable int sceneNumber,
            @RequestBody SceneUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("씬이 수정되었습니다.", plotService.updateScene(plotId, sceneNumber, request)));
    }

    /**
     * 플롯 삭제
     *
     * <p>연결된 이미지·영상도 CASCADE 삭제된다.</p>
     *
     * @param plotId 삭제할 플롯 UUID
     * @return 200 OK
     */
    @DeleteMapping("/{plotId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String plotId) {
        plotService.delete(plotId);
        return ResponseEntity.ok(ApiResponse.ok("플롯이 삭제되었습니다.", null));
    }
}
