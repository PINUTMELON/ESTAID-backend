package com.estaid.project;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRatingRepository extends JpaRepository<ProjectRating, String> {
    Optional<ProjectRating> findByProject_ProjectIdAndUserId(String projectId, String userId);

    long countByProject_ProjectId(String projectId);

    @Query("""
            select coalesce(sum(r.rating), 0)
            from ProjectRating r
            where r.project.projectId = :projectId
            """)
    Integer sumRatingsByProjectId(@Param("projectId") String projectId);
}
