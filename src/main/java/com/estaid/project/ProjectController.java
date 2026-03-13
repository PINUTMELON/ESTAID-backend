package com.estaid.project;

import com.estaid.common.response.ApiResponse;
import com.estaid.project.dto.ProjectRequest;
import com.estaid.project.dto.ProjectResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 프로젝트 REST 컨트롤러
 *
 * <p>기본 경로: {@code /api/projects}</p>
 *
 * <ul>
 *   <li>GET    /api/projects           - 전체 목록 조회</li>
 *   <li>GET    /api/projects/{id}      - 단건 조회</li>
 *   <li>POST   /api/projects           - 생성</li>
 *   <li>PUT    /api/projects/{id}      - 수정</li>
 *   <li>DELETE /api/projects/{id}      - 삭제</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 프로젝트 전체 목록 조회
     *
     * @return 200 OK + 프로젝트 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(projectService.findAll()));
    }

    /**
     * 프로젝트 단건 조회
     *
     * @param projectId 조회할 프로젝트 UUID
     * @return 200 OK + 프로젝트 정보
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> findById(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.findById(projectId)));
    }

    /**
     * 프로젝트 생성
     *
     * @param request 생성 요청 바디 (title 필수)
     * @return 201 Created + 생성된 프로젝트 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> create(@Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("프로젝트가 생성되었습니다.", response));
    }

    /**
     * 프로젝트 수정
     *
     * @param projectId 수정할 프로젝트 UUID
     * @param request   수정 요청 바디
     * @return 200 OK + 수정된 프로젝트 정보
     */
    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("프로젝트가 수정되었습니다.", projectService.update(projectId, request)));
    }

    /**
     * 프로젝트 삭제
     *
     * <p>연결된 캐릭터·플롯·이미지·영상도 CASCADE 삭제된다.</p>
     *
     * @param projectId 삭제할 프로젝트 UUID
     * @return 200 OK
     */
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String projectId) {
        projectService.delete(projectId);
        return ResponseEntity.ok(ApiResponse.ok("프로젝트가 삭제되었습니다.", null));
    }
}
