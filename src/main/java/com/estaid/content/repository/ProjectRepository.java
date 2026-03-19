package com.estaid.content.repository;

import com.estaid.content.entity.ProjectEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** projects read repository. */
public interface ProjectRepository extends JpaRepository<ProjectEntity, String> {
    List<ProjectEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<ProjectEntity> findByUserIdNotOrderByCreatedAtDesc(String userId);

    Optional<ProjectEntity> findByProjectIdAndUserId(String projectId, String userId);
}
