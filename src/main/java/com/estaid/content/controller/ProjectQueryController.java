package com.estaid.content.controller;

import com.estaid.auth.service.AuthenticatedUserService;
import com.estaid.common.response.ApiResponse;
import com.estaid.content.dto.ProjectDetailResponse;
import com.estaid.content.dto.ProjectRankingResponse;
import com.estaid.content.dto.ProjectScenesResponse;
import com.estaid.content.dto.VideoPageInitResponse;
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

    @GetMapping("/ranking")
    public ApiResponse<List<ProjectRankingResponse>> getProjectRanking(HttpServletRequest request) {
        authenticatedUserService.requireCurrentUserId(request);
        return ApiResponse.ok(contentQueryService.getProjectRanking());
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

    /**
     * 영상 페이지 초기 정보 조회
     *
     * <p>영상 생성 페이지 진입 시 씬별 프레임 이미지 URL과 통합 프롬프트를 반환한다.
     * 프론트엔드는 이 정보를 사용해 영상 생성 요청({@code POST /api/videos/generate})을 구성한다.</p>
     *
     * @param projectId 프로젝트 UUID (경로 변수)
     * @param request   HTTP 요청 (JWT 사용자 인증용)
     * @return 영상 페이지 초기 정보 (씬 번호 오름차순)
     */
    @GetMapping("/{id}/video-info")
    public ApiResponse<VideoPageInitResponse> getVideoPageInfo(
            @PathVariable("id") String projectId,
            HttpServletRequest request) {
        String userId = authenticatedUserService.requireCurrentUserId(request);
        return ApiResponse.ok(contentQueryService.getVideoPageInfo(projectId, userId));
    }
}
