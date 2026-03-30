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

    /**
     * 평점 합계와 개수를 한 번의 쿼리로 조회한다 (2번 → 1번으로 최적화).
     * 결과: Object[]{sum(Integer), count(Long)}
     */
    @Query("""
            select coalesce(sum(r.rating), 0), count(r)
            from ProjectRating r
            where r.project.projectId = :projectId
            """)
    Object[] sumAndCountByProjectId(@Param("projectId") String projectId);
}
