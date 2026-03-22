package com.estaid.project;

import com.estaid.common.exception.BusinessException;
import com.estaid.project.dto.ProjectCreateRequest;
import com.estaid.project.dto.ProjectRatingRequest;
import com.estaid.project.dto.ProjectRatingResponse;
import com.estaid.project.dto.ProjectRequest;
import com.estaid.project.dto.ProjectResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ProjectRatingRepository projectRatingRepository;

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
    public ProjectResponse create(ProjectCreateRequest request, String userId) {
        Project project = Project.builder()
                .title(request.getTitle())
                .userId(userId)
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

        log.info("프로젝트 수정 완료: projectId={}, userId={}", projectId, userId);
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectRatingResponse addRating(String projectId, ProjectRatingRequest request, String userId) {
        Project project = projectRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(
                        "프로젝트를 찾을 수 없습니다. id=" + projectId,
                        HttpStatus.NOT_FOUND));

        ProjectRating projectRating = projectRatingRepository.findByProject_ProjectIdAndUserId(projectId, userId)
                .orElseGet(() -> ProjectRating.builder()
                        .project(project)
                        .userId(userId)
                        .build());

        projectRating.setRating(request.rating());
        projectRatingRepository.save(projectRating);

        int nextRatingSum = projectRatingRepository.sumRatingsByProjectId(projectId);
        int nextRatingCount = Math.toIntExact(projectRatingRepository.countByProject_ProjectId(projectId));
        BigDecimal nextAverageRating = BigDecimal.valueOf(nextRatingSum)
                .divide(BigDecimal.valueOf(nextRatingCount), 2, RoundingMode.HALF_UP);

        project.setRatingSum(nextRatingSum);
        project.setRatingCount(nextRatingCount);
        project.setAverageRating(nextAverageRating);

        log.info("프로젝트 평점 반영 완료: projectId={}, userId={}, rating={}, averageRating={}, ratingCount={}",
                projectId, userId, request.rating(), nextAverageRating, nextRatingCount);
        return ProjectRatingResponse.from(project);
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
