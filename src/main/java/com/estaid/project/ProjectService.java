package com.estaid.project;

import com.estaid.common.exception.BusinessException;
import com.estaid.project.dto.ProjectRequest;
import com.estaid.project.dto.ProjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 프로젝트 비즈니스 로직 서비스
 *
 * <p>프로젝트 생성·조회·수정·삭제(CRUD)를 담당한다.
 * 트랜잭션 경계는 메서드 단위로 설정하며,
 * 조회 메서드는 readOnly = true로 성능을 최적화한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    /**
     * 프로젝트 전체 목록 조회
     *
     * @return 전체 프로젝트 목록 (생성된 순서대로 반환됨)
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll() {
        return projectRepository.findAll()
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    /**
     * 프로젝트 단건 조회
     *
     * @param projectId 조회할 프로젝트 UUID
     * @return 프로젝트 응답 DTO
     * @throws BusinessException 해당 ID의 프로젝트가 없을 때 (404)
     */
    @Transactional(readOnly = true)
    public ProjectResponse findById(String projectId) {
        Project project = getProjectOrThrow(projectId);
        return ProjectResponse.from(project);
    }

    /**
     * 프로젝트 생성
     *
     * @param request 프로젝트 생성 요청 DTO
     * @return 생성된 프로젝트 응답 DTO
     */
    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        Project project = Project.builder()
                .title(request.getTitle())
                .backgroundImageUrl(request.getBackgroundImageUrl())
                .settingsJson(request.getSettingsJson())
                .build();

        Project saved = projectRepository.save(project);
        log.info("프로젝트 생성 완료: projectId={}, title={}", saved.getProjectId(), saved.getTitle());
        return ProjectResponse.from(saved);
    }

    /**
     * 프로젝트 수정
     *
     * <p>전달된 필드로 기존 값을 덮어쓴다.
     * null인 필드도 그대로 반영되므로, 클라이언트는 수정하지 않는 필드도 기존 값을 함께 전송해야 한다.</p>
     *
     * @param projectId 수정할 프로젝트 UUID
     * @param request   수정 요청 DTO
     * @return 수정된 프로젝트 응답 DTO
     * @throws BusinessException 해당 ID의 프로젝트가 없을 때 (404)
     */
    @Transactional
    public ProjectResponse update(String projectId, ProjectRequest request) {
        Project project = getProjectOrThrow(projectId);

        project.setTitle(request.getTitle());
        project.setBackgroundImageUrl(request.getBackgroundImageUrl());
        project.setSettingsJson(request.getSettingsJson());

        log.info("프로젝트 수정 완료: projectId={}", projectId);
        return ProjectResponse.from(project);
    }

    /**
     * 프로젝트 삭제
     *
     * <p>DB에 ON DELETE CASCADE가 설정되어 있어
     * 프로젝트 삭제 시 연결된 캐릭터·플롯·이미지·영상도 함께 삭제된다.</p>
     *
     * @param projectId 삭제할 프로젝트 UUID
     * @throws BusinessException 해당 ID의 프로젝트가 없을 때 (404)
     */
    @Transactional
    public void delete(String projectId) {
        Project project = getProjectOrThrow(projectId);
        projectRepository.delete(project);
        log.info("프로젝트 삭제 완료: projectId={}", projectId);
    }

    /**
     * 프로젝트 조회 공통 메서드 - 없으면 예외 발생
     *
     * @param projectId 조회할 프로젝트 UUID
     * @return 프로젝트 엔티티
     * @throws BusinessException 프로젝트가 존재하지 않을 때 (404)
     */
    private Project getProjectOrThrow(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        "프로젝트를 찾을 수 없습니다. id=" + projectId,
                        HttpStatus.NOT_FOUND
                ));
    }
}
