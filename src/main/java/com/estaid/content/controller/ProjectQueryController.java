package com.estaid.content.controller;

import com.estaid.auth.service.AuthenticatedUserService;
import com.estaid.common.response.ApiResponse;
import com.estaid.content.dto.ProjectDetailResponse;
import com.estaid.content.dto.ProjectScenesResponse;
import com.estaid.content.service.ContentQueryService;
import com.estaid.project.ProjectService;
import com.estaid.project.dto.ProjectResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectQueryController {

    private final ContentQueryService contentQueryService;
    private final ProjectService projectService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    public ApiResponse<List<ProjectResponse>> getMyProjects(HttpServletRequest request) {
        String userId = authenticatedUserService.requireCurrentUserId(request);
        return ApiResponse.ok(projectService.findAllByUserId(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectDetailResponse> getProject(
            @PathVariable("id") String projectId,
            HttpServletRequest request) {
        String userId = authenticatedUserService.requireCurrentUserId(request);
        return ApiResponse.ok(contentQueryService.getProjectDetail(projectId, userId));
    }

    @GetMapping("/{id}/scenes")
    public ApiResponse<ProjectScenesResponse> getProjectScenes(
            @PathVariable("id") String projectId,
            HttpServletRequest request) {
        String userId = authenticatedUserService.requireCurrentUserId(request);
        return ApiResponse.ok(contentQueryService.getProjectScenes(projectId, userId));
    }
}
