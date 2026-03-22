package com.estaid.content.repository;

import com.estaid.content.entity.ProjectEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 프로젝트 엔티티 저장소 (조회 전용). */
@Repository("contentProjectRepository")
public interface ProjectRepository extends JpaRepository<ProjectEntity, String> {
    List<ProjectEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<ProjectEntity> findByUserIdNotOrderByCreatedAtDesc(String userId);

    List<ProjectEntity> findAllByOrderByAverageRatingDescRatingCountDescCreatedAtDesc();

    Optional<ProjectEntity> findByProjectIdAndUserId(String projectId, String userId);
}
