package com.estaid.project.dto;

import com.estaid.project.Project;
import java.math.BigDecimal;

public record ProjectRatingResponse(
        String projectId,
        BigDecimal averageRating,
        Integer ratingCount,
        Integer ratingSum
) {
    public static ProjectRatingResponse from(Project project) {
        return new ProjectRatingResponse(
                project.getProjectId(),
                project.getAverageRating(),
                project.getRatingCount(),
                project.getRatingSum());
    }
}
