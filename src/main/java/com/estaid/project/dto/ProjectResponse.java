package com.estaid.project.dto;

import com.estaid.project.Project;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectResponse {

    private String projectId;
    private String title;
    private String backgroundImageUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
                .projectId(project.getProjectId())
                .title(project.getTitle())
                .backgroundImageUrl(project.getBackgroundImageUrl())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
