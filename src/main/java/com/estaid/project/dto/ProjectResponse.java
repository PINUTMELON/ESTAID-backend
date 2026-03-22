package com.estaid.project.dto;

import com.estaid.project.Project;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectResponse {

    private String projectId;
    private String title;
    private String backgroundImageUrl;
    private BigDecimal averageRating;
    private Integer ratingCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
                .projectId(project.getProjectId())
                .title(project.getTitle())
                .backgroundImageUrl(project.getBackgroundImageUrl())
                .averageRating(project.getAverageRating())
                .ratingCount(project.getRatingCount())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
