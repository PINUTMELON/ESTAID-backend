package com.estaid.project.dto;

import com.estaid.project.Project;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * 프로젝트 응답 DTO
 *
 * <p>API 응답 시 {@link Project} 엔티티를 직접 노출하지 않고
 * 필요한 필드만 골라 클라이언트에 전달한다.</p>
 */
@Getter
@Builder
public class ProjectResponse {

    /** 프로젝트 고유 식별자 (UUID) */
    private String projectId;

    /** 프로젝트 제목 */
    private String title;

    /** 배경 이미지 URL */
    private String backgroundImageUrl;

    /** AI 영상 생성 기본 설정 (JSON 문자열) */
    private String settingsJson;

    /** 생성 시각 */
    private OffsetDateTime createdAt;

    /** 최종 수정 시각 */
    private OffsetDateTime updatedAt;

    /**
     * {@link Project} 엔티티를 {@link ProjectResponse} DTO로 변환한다.
     *
     * @param project 변환할 프로젝트 엔티티
     * @return ProjectResponse DTO
     */
    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
                .projectId(project.getProjectId())
                .title(project.getTitle())
                .backgroundImageUrl(project.getBackgroundImageUrl())
                .settingsJson(project.getSettingsJson())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
