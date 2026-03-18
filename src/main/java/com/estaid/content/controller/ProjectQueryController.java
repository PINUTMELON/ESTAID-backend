package com.estaid.content.controller;

import com.estaid.common.response.ApiResponse;
import com.estaid.content.dto.ProjectInfoResponse;
import com.estaid.content.dto.ProjectScenesResponse;
import com.estaid.content.service.ContentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 프로젝트 조회 API를 제공한다. */
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectQueryController {

    private final ContentQueryService contentQueryService;

    /** 프로젝트 기본 정보를 조회한다. */
    @GetMapping("/{id}")
    public ApiResponse<ProjectInfoResponse> getProject(@PathVariable("id") String projectId) {
        return ApiResponse.ok(contentQueryService.getProjectInfo(projectId));
    }

    /** 프로젝트별 플롯/씬 요약 정보를 조회한다. */
    @GetMapping("/{id}/scenes")
    public ApiResponse<ProjectScenesResponse> getProjectScenes(@PathVariable("id") String projectId) {
        return ApiResponse.ok(contentQueryService.getProjectScenes(projectId));
    }
}
