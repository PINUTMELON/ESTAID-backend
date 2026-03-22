package com.estaid.project;

import com.estaid.auth.service.AuthenticatedUserService;
import com.estaid.common.response.ApiResponse;
import com.estaid.project.dto.ProjectRatingRequest;
import com.estaid.project.dto.ProjectRatingResponse;
import com.estaid.project.dto.ProjectRequest;
import com.estaid.project.dto.ProjectResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> findAll(HttpServletRequest request) {
        String userId = authenticatedUserService.requireCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(projectService.findAllByUserId(userId)));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> findById(
            @PathVariable String projectId,
            HttpServletRequest request) {
        String userId = authenticatedUserService.requireCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(projectService.findById(projectId, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @Valid @RequestBody ProjectRequest request,
            HttpServletRequest httpServletRequest) {
        String userId = authenticatedUserService.requireCurrentUserId(httpServletRequest);
        ProjectResponse response = projectService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("프로젝트가 생성되었습니다.", response));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectRequest request,
            HttpServletRequest httpServletRequest) {
        String userId = authenticatedUserService.requireCurrentUserId(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(
                "프로젝트가 수정되었습니다.",
                projectService.update(projectId, request, userId)));
    }

    @PostMapping("/{projectId}/rating")
    public ResponseEntity<ApiResponse<ProjectRatingResponse>> addRating(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectRatingRequest request,
            HttpServletRequest httpServletRequest) {
        String userId = authenticatedUserService.requireCurrentUserId(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(
                "프로젝트 평점이 반영되었습니다.",
                projectService.addRating(projectId, request, userId)));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String projectId,
            HttpServletRequest request) {
        String userId = authenticatedUserService.requireCurrentUserId(request);
        projectService.delete(projectId, userId);
        return ResponseEntity.ok(ApiResponse.ok("프로젝트가 삭제되었습니다.", null));
    }
}
