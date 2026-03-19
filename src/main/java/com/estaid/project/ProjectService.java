package com.estaid.project;

import com.estaid.common.exception.BusinessException;
import com.estaid.project.dto.ProjectRequest;
import com.estaid.project.dto.ProjectResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAllByUserId(String userId) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(String projectId, String userId) {
        return ProjectResponse.from(getOwnedProjectOrThrow(projectId, userId));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request, String userId) {
        Project project = Project.builder()
                .title(request.getTitle())
                .userId(userId)
                .backgroundImageUrl(request.getBackgroundImageUrl())
                .settingsJson(request.getSettingsJson())
                .build();

        Project saved = projectRepository.save(project);
        log.info("프로젝트 생성 완료: projectId={}, title={}, userId={}",
                saved.getProjectId(), saved.getTitle(), userId);
        return ProjectResponse.from(saved);
    }

    @Transactional
    public ProjectResponse update(String projectId, ProjectRequest request, String userId) {
        Project project = getOwnedProjectOrThrow(projectId, userId);

        project.setTitle(request.getTitle());
        project.setBackgroundImageUrl(request.getBackgroundImageUrl());
        project.setSettingsJson(request.getSettingsJson());

        log.info("프로젝트 수정 완료: projectId={}, userId={}", projectId, userId);
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(String projectId, String userId) {
        Project project = getOwnedProjectOrThrow(projectId, userId);
        projectRepository.delete(project);
        log.info("프로젝트 삭제 완료: projectId={}, userId={}", projectId, userId);
    }

    private Project getOwnedProjectOrThrow(String projectId, String userId) {
        return projectRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(
                        "프로젝트를 찾을 수 없습니다. id=" + projectId,
                        HttpStatus.NOT_FOUND));
    }
}
